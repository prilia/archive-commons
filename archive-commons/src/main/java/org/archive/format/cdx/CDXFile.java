package org.archive.format.cdx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.archive.streamcontext.Stream;
import org.archive.streamcontext.StreamWrappedInputStream;
import org.archive.util.GeneralURIStreamFactory;
import org.archive.util.binsearch.SeekableLineReaderFactory;
import org.archive.util.binsearch.SortedTextFile;
import org.archive.util.binsearch.impl.RandomAccessFileSeekableLineReaderFactory;
import org.archive.util.iterator.CloseableIterator;
import org.archive.util.zip.OpenJDK7GZIPInputStream;

public class CDXFile extends SortedTextFile implements CDXInputSource {

	public CDXFile(String uri) throws IOException {
		super(getUriFactory(uri, false));
	}

	public CDXFile(SeekableLineReaderFactory factory) {
		super(factory);
	}

	public CloseableIterator<String> getCDXLineIterator(String key, String prefix) throws IOException {
		return getRecordIteratorLT(key);
	}
	
	public static SeekableLineReaderFactory getUriFactory(String uri, boolean decodeToTemp) throws IOException
	{
		if (decodeToTemp) {
			return new RandomAccessFileSeekableLineReaderFactory(decodeGZToTemp(uri));
		}
		
		return GeneralURIStreamFactory.createSeekableStreamFactory(uri, false);
	}
	
	// Decode gzipped cdx to a temporary file	
	public static File decodeGZToTemp(String uriGZ) throws IOException {
		final int BUFFER_SIZE = 8192;

		Stream stream = null;

		try {
			stream = GeneralURIStreamFactory.createStream(uriGZ);
			InputStream input = new StreamWrappedInputStream(stream);
			input = new OpenJDK7GZIPInputStream(input);

			File uncompressedCdx = File.createTempFile(uriGZ, ".cdx");
			FileOutputStream out = new FileOutputStream(uncompressedCdx, false);

			byte buff[] = new byte[BUFFER_SIZE];
			int numRead = 0;
			
			while ((numRead = input.read(buff)) > 0) {
				out.write(buff, 0, numRead);
			}
			
			out.flush();
			out.close();
			
			return uncompressedCdx;
			
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}
	
	public static BufferedReader createStreamingLineReader(String uri, boolean gzipped) throws IOException
	{
		Stream stream = GeneralURIStreamFactory.createStream(uri);
		StreamWrappedInputStream swis = new StreamWrappedInputStream(stream);
		swis.setCloseOnClose(true);
		
		InputStream input = swis;
		
		if (gzipped) {
			input = new OpenJDK7GZIPInputStream(swis);	
		}
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		return reader;
	}
}
