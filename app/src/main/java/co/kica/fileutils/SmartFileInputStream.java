package co.kica.fileutils;

import co.kica.fileutils.SmartFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

public class SmartFileInputStream extends InputStream {
  private static HashMap cache = new HashMap();
  private byte[] buffer = null;
  private long byteptr = 0L;
  private SmartFile file;
  private long size;

  public SmartFileInputStream(SmartFile var1) throws FileNotFoundException {
    this.file = var1;
    if(var1.exists() && var1.isFile()) {
      this.size = this.file.length();
      this.byteptr = 0L;
      this.precache();
    } else {
      throw new FileNotFoundException(this.file.getAbsolutePath() + " (not a valid smartfile)");
    }
  }

  public SmartFileInputStream(String var1) throws FileNotFoundException {
    SmartFile var2 = new SmartFile(var1);
    this.file = var2;
    if(var2.exists() && var2.isFile()) {
      this.size = this.file.length();
      this.byteptr = 0L;
      this.precache();
    } else {
      throw new FileNotFoundException(this.file.getAbsolutePath() + " (not a valid smartfile)");
    }
  }

  private void precache() {
    this.buffer = this.file.getBuffer();
    System.out.println("precache(): sitting on " + this.buffer.length);
    this.size = (long)this.buffer.length;
  }

  public int available() {
    return (int)(this.size - this.byteptr);
  }

  public int read() throws IOException {
    int var1;
    if(this.byteptr < this.size && this.byteptr >= 0L) {
      byte[] var4 = this.buffer;
      long var2 = this.byteptr;
      this.byteptr = 1L + var2;
      var1 = var4[(int)var2] & 255;
    } else {
      var1 = -1;
    }

    return var1;
  }

  public int read(byte[] var1) throws IOException {
    int var2;
    if(this.byteptr >= this.size) {
      var2 = -1;
    } else {
      long var7;
      if(this.size - this.byteptr >= (long)var1.length) {
        var7 = (long)var1.length;
      } else {
        var7 = this.size - this.byteptr;
      }

      int var4 = (int)var7;
      System.out.println("Requested " + var1.length + ", can give it " + var4);
      if(var4 == 0) {
        var2 = -1;
      } else {
        byte[] var9 = Arrays.copyOfRange(this.buffer, (int)this.byteptr, (int)(this.byteptr + (long)var4));
        int var3 = 0;
        int var5 = var9.length;

        for(var2 = 0; var2 < var5; ++var2) {
          byte var10000 = var9[var2];
          var1[var3] = var9[var3];
          ++var3;
        }

        this.byteptr += (long)var4;
        var2 = var4;
      }
    }

    return var2;
  }

  public int read(byte[] var1, int var2, int var3) throws IOException {
    int var4 = var3;
    if(var3 - var2 >= var1.length) {
      var4 = var1.length - var2;
    }

    var3 = var4;
    if(var4 < 0) {
      var3 = 0;
    }

    var4 = var2;

    int var5;
    for(var5 = 0; var4 < var2 + var3; ++var5) {
      int var6 = this.read();
      if(var6 < 0) {
        break;
      }

      var1[var5] = (byte)(var6 & 255);
      ++var4;
    }

    return var5;
  }

  public void reset() {
    this.byteptr = 0L;
  }
}
