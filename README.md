# Drumbeat
A minimal [SCORE](https://github.com/overture-stack/score) replacement. This
allows using a local file system as a way to upload files using SONG. It is
meant to be used in conjunction with FTP, SFTP, or network shares.

## Configuration

Create a JSON configuration file:

    {
      "incoming": [
        "/srv/incoming/1",
        "/srv/incoming/2"
      ],
      "port": 8080,
      "splits": [
        2,
        4,
        4
      ],
      "storage": "/srv/data"
    }

The server will run an HTTP server on `"port"`.

The server will look for files in the `"incoming"` directories. Users will
upload data into these directories using SFTP or some other method.

`"storage"` defines the final directory where output will be kept. It will be
copied to this directory to ensure it has correct ownership. The file names in
the input directory are the full SONG identifiers.

To avoid having too many files in the storage directory, the file name will be
split into chunks. The `"splits"` list describes how to chunk the identifiers
into the directory names. In this example, the first directory will contain two
characters of the object ID, the next layer will have four characters, then
another four characters.

For example, `abcdefghijklmnop` would be converted to
`/srv/data/ab/cdef/ghij/klmnop`.

Run the server:

    java ca.on.oicr.gsi.drumbeat.Server /path/to/confguration.json

## Building
Java 11 is required for building and running Drumbeat. The server can be built
with Maven:

    mvn install

It maybe useful to collect all the dependencies into a single directory to put on the module path using:

    mvn dependency:copy-dependencies
