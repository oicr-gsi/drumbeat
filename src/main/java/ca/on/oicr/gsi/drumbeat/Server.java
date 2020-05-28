package ca.on.oicr.gsi.drumbeat;

import ca.on.oicr.gsi.status.ConfigurationSection;
import ca.on.oicr.gsi.status.Header;
import ca.on.oicr.gsi.status.NavigationMenu;
import ca.on.oicr.gsi.status.SectionRenderer;
import ca.on.oicr.gsi.status.ServerConfig;
import ca.on.oicr.gsi.status.StatusPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Server implements ServerConfig {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println(
          "Usage: java --module-path MODULES --module ca.on.oicr.gsi.drumbeat configuration.json");
    }
    DefaultExports.initialize();
    final var configuration = MAPPER.readValue(new File(args[0]), Configuration.class);
    final var server = new Server(configuration);
    final var undertow =
        Undertow.builder()
            .addHttpListener(configuration.getPort(), "0.0.0.0")
            .setWorkerThreads(10 * Runtime.getRuntime().availableProcessors())
            .setHandler(
                Handlers.routing()
                    .get("/", new BlockingHandler(server::status))
                    .get("/download/{id}", new BlockingHandler(server::download))
                    .get("/metrics", new BlockingHandler(server::metrics))
                    .get("/upload/{id}", new BlockingHandler(server::upload)))
            .build();
    undertow.start();
  }

  private static Optional<String> pathParam(HttpServerExchange exchange, String name) {
    return Optional.ofNullable(exchange.getQueryParameters().get(name)).map(Deque::getFirst);
  }

  private final List<Path> incoming;
  private final List<Integer> splits;
  private final Path storage;
  private final StatusPage status =
      new StatusPage(this) {
        @Override
        protected void emitCore(SectionRenderer sectionRenderer) {
          sectionRenderer.line("Storage Directory", storage.toString());
          for (final var directory : incoming) {
            sectionRenderer.line("Incoming Directory", directory.toString());
          }
        }

        @Override
        public Stream<ConfigurationSection> sections() {
          return Stream.empty();
        }
      };

  private Server(Configuration configuration) {
    incoming = configuration.getIncoming().stream().map(Paths::get).collect(Collectors.toList());
    splits = configuration.getSplits();
    storage = Paths.get(configuration.getStorage());
  }

  private void download(HttpServerExchange httpServerExchange) {
    pathParam(httpServerExchange, "id")
        .ifPresentOrElse(
            name -> {
              final var dataFile = storagePathForObject(name);
              final var metadataFile = dataFile.resolveSibling(dataFile.getFileName() + ".spec");
              if (Files.exists(metadataFile)) {
                // This file has been previously processed. We can simply feed it the old metadata.
                httpServerExchange
                    .getResponseHeaders()
                    .put(Headers.CONTENT_TYPE, "application/json");
                httpServerExchange.setStatusCode(StatusCodes.OK);
                try {
                  final var channel = FileChannel.open(metadataFile);
                  httpServerExchange
                      .getResponseSender()
                      .transferFrom(
                          channel,
                          new IoCallback() {
                            @Override
                            public void onComplete(
                                HttpServerExchange httpServerExchange, Sender sender) {
                              try {
                                channel.close();
                              } catch (IOException e) {
                                e.printStackTrace();
                              }
                              IoCallback.END_EXCHANGE.onComplete(httpServerExchange, sender);
                            }

                            @Override
                            public void onException(
                                HttpServerExchange httpServerExchange,
                                Sender sender,
                                IOException e) {
                              try {
                                channel.close();
                              } catch (IOException ex) {
                                ex.printStackTrace();
                              }
                              IoCallback.END_EXCHANGE.onException(httpServerExchange, sender, e);
                            }
                          });
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              } else {

                httpServerExchange.setStatusCode(StatusCodes.NOT_FOUND);
                httpServerExchange.getResponseSender().send("No such file.");
              }
            },
            () -> {
              httpServerExchange.setStatusCode(StatusCodes.BAD_REQUEST);
              httpServerExchange.getResponseSender().send("Invalid object key.");
            });
  }

  @Override
  public Stream<Header> headers() {
    return Stream.empty();
  }

  private void metrics(HttpServerExchange httpServerExchange) {
    httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TextFormat.CONTENT_TYPE_004);
    httpServerExchange.setStatusCode(200);

    try (final var os = httpServerExchange.getOutputStream();
        final var writer = new PrintWriter(os)) {
      TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public String name() {
    return "Drumbeat";
  }

  @Override
  public Stream<NavigationMenu> navigation() {
    return Stream.empty();
  }

  private boolean processFile(String name) {
    final var dataFile = storagePathForObject(name);
    final var metadataFile = dataFile.resolveSibling(dataFile.getFileName() + ".spec");
    if (Files.exists(metadataFile)) {
      return true;
    } else {
      return incoming.stream()
          .map(i -> i.resolve(name))
          .filter(Files::exists)
          .findAny()
          .map(
              incomingFile -> {
                // File has been uploaded but not processed; copy it to the output
                // location, generating the metadata file, and delete the original
                try {
                  Files.createDirectories(dataFile.getParent());
                  try (final var input = FileChannel.open(incomingFile);
                      final var output =
                          new DigestByteChannel(
                              FileChannel.open(
                                  dataFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE));
                      final var metadataOutput = new FileOutputStream(metadataFile.toFile())) {
                    input.transferTo(0, input.size(), output);
                    final var md5 = output.digest();
                    final var part = new Part();
                    part.setMd5(md5);
                    part.setOffset(0);
                    part.setPartNumber(0);
                    part.setPartSize(input.size());
                    part.setSourceMd5(md5);
                    part.setUrl("file://" + dataFile);
                    final var spec = new ObjectSpecification();
                    spec.setObjectId(name);
                    spec.setObjectKey(name);
                    spec.setObjectMd5(md5);
                    spec.setObjectSize(input.size());
                    spec.setParts(List.of(part));
                    spec.setUploadId("local");
                    MAPPER.writeValue(metadataOutput, spec);
                  }
                  Files.delete(incomingFile);
                  return false;
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              })
          .orElse(false);
    }
  }

  private void status(HttpServerExchange httpServerExchange) {
    httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
    httpServerExchange.setStatusCode(StatusCodes.OK);
    httpServerExchange.startBlocking();
    status.renderPage(httpServerExchange.getOutputStream());
  }

  private Path storagePathForObject(String id) {
    var result = storage;
    int index = 0;
    for (var split = 0; split < splits.size() && index < id.length(); split++) {
      var end = Math.min(id.length(), index + splits.get(split));
      result = result.resolve(id.substring(index, end));
      index = end;
    }
    if (index < id.length()) {
      result = result.resolve(id.substring(index));
    }
    return result;
  }

  private void upload(HttpServerExchange httpServerExchange) {
    pathParam(httpServerExchange, "id")
        .map(this::processFile)
        .ifPresentOrElse(
            result -> {
              httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
              try {
                httpServerExchange.getResponseSender().send(MAPPER.writeValueAsString(result));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            },
            () -> {
              httpServerExchange.setStatusCode(StatusCodes.BAD_REQUEST);
              httpServerExchange.getResponseSender().send("Invalid object key.");
            });
  }
}
