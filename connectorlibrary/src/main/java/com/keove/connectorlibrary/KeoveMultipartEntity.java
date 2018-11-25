package com.keove.connectorlibrary;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

//import org.apache.http.entity.mime.HttpMultipartMode;
//import org.apache.http.entity.mime.MultipartEntity;


@SuppressWarnings("deprecation")
public class KeoveMultipartEntity //extends MultipartEntity
{


    public interface MultipartProgressListener
    {
        public abstract void transferred(long num);
    }

    private MultipartProgressListener multipartProgressListener;
    public void setMultipartProgressListener(MultipartProgressListener multipartProgressListener)
    {
        this.multipartProgressListener = multipartProgressListener;
    }

    /*
    public KeoveMultipartEntity()
    {
        super();
    }

    public KeoveMultipartEntity(HttpMultipartMode mode)
    {
        super(mode);
    }

    public KeoveMultipartEntity(HttpMultipartMode mode, final String boundary, Charset charset)
    {
        super(mode,boundary,charset);
    }


    @Override
    public void writeTo(OutputStream outstream) throws IOException
    {
        CountingOutputStream cos = new CountingOutputStream(outstream);
        cos.setMultipartProgressListener(multipartProgressListener);
        super.writeTo(cos);
    }


    */



    public static class CountingOutputStream extends FilterOutputStream
    {

        private MultipartProgressListener multipartProgressListener;

        public void setMultipartProgressListener(MultipartProgressListener multipartProgressListener) {
            this.multipartProgressListener = multipartProgressListener;
        }

        private long transferred;

        public CountingOutputStream(OutputStream out) {
            super(out);
            this.transferred = 0;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            this.transferred += len;
            if(multipartProgressListener!=null) this.multipartProgressListener.transferred(this.transferred);
        }

        public void write(int b) throws IOException {
            out.write(b);
            this.transferred++;
            if(multipartProgressListener!=null) this.multipartProgressListener.transferred(this.transferred);
        }
    }
}
