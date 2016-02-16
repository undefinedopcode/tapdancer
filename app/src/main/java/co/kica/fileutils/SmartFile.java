package co.kica.fileutils;

import co.kica.tap.T64Format;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class SmartFile extends File {
    private static final long serialVersionUID = 491916781031060975L;
    private boolean T64 = false;
    private String path = "";
    private byte[] shadowFileReader;
    private File shadowSelf;
    private T64Format shadowT64;
    private T64Format.DirEntry shadowT64Entry;
    private ZipFile shadowZIP;
    private ZipEntry shadowZIPEntry;
    private String subpath = "";
    private SmartFile.SmartType type;

    public SmartFile(File var1, String var2) {
        super(var1, var2);
        this.type = SmartFile.SmartType.PHYSICAL;
        this.shadowFileReader = null;
        this.shadowT64Entry = null;
        this.shadowZIPEntry = null;
        this.smartBreak(this.getAbsolutePath());
    }

    public SmartFile(String var1) {
        super(var1);
        this.type = SmartFile.SmartType.PHYSICAL;
        this.shadowFileReader = null;
        this.shadowT64Entry = null;
        this.shadowZIPEntry = null;
        this.smartBreak(this.getAbsolutePath());
    }

    public SmartFile(String var1, String var2) {
        super(var1, var2);
        this.type = SmartFile.SmartType.PHYSICAL;
        this.shadowFileReader = null;
        this.shadowT64Entry = null;
        this.shadowZIPEntry = null;
        this.smartBreak(this.getAbsolutePath());
    }

    public SmartFile(URI var1) {
        super(var1);
        this.type = SmartFile.SmartType.PHYSICAL;
        this.shadowFileReader = null;
        this.shadowT64Entry = null;
        this.shadowZIPEntry = null;
        this.smartBreak(this.getAbsolutePath());
    }

    private T64Format.DirEntry entryT64(String var1, String var2) {
        Iterator var3 = this.shadowT64.getDir().iterator();

        T64Format.DirEntry var4;
        do {
            if(!var3.hasNext()) {
                var4 = null;
                break;
            }

            var4 = (T64Format.DirEntry)var3.next();
        } while(!var4.getFilename().replaceAll("[ ]+$", "").equals(var2));

        return var4;
    }

    private byte[] extractFile(ZipInputStream var1) throws IOException {
        byte[] var6 = new byte[(int)this.length()];
        byte[] var5 = new byte[512];
        int var2 = 0;

        while(true) {
            int var4 = var1.read(var5);
            if(var4 == -1) {
                return var6;
            }

            for(int var3 = 0; var3 < var4; ++var3) {
                var6[var2 + var3] = var5[var3];
            }

            var2 += var4;
        }
    }

    private long sizeT64() {
        Iterator var3 = this.shadowT64.getDir().iterator();

        long var1;
        while(true) {
            if(!var3.hasNext()) {
                var1 = 0L;
                break;
            }

            T64Format.DirEntry var4 = (T64Format.DirEntry)var3.next();
            if(var4.getFilename().equals(this.subpath)) {
                var1 = (long)(var4.getSize() + 2);
                break;
            }
        }

        return var1;
    }

    private long sizeZIP() {
        ZipEntry var3 = this.shadowZIP.getEntry(this.subpath);
        long var1;
        if(var3 == null) {
            var1 = 0L;
        } else {
            var1 = var3.getSize();
        }

        return var1;
    }

    private void smartBreak(String var1) {
        String var5 = "";
        File var4 = new File(var1);
        String var3 = var1;

        for(var1 = var5; !var4.exists() && var3.length() > 0; var4 = new File(var3)) {
            String[] var9 = var3.split(File.separator);
            if(var1.length() == 0) {
                var1 = var9[var9.length - 1];
            } else {
                var1 = var9[var9.length - 1] + File.separator + var1;
            }

            var3 = "";

            for(int var2 = 0; var2 < var9.length - 1; ++var2) {
                if(var2 == 0) {
                    var3 = "";
                } else {
                    var3 = var3 + File.separator + var9[var2];
                }
            }
        }

        this.path = var3;
        this.subpath = var1;
        this.shadowSelf = new File(this.path);
        if(this.path.toLowerCase().endsWith(".t64") && this.T64) {
            this.shadowT64 = new T64Format(this.path, false);
            if(this.shadowT64.validHeader()) {
                this.setType(SmartFile.SmartType.T64FILE);
            }
        }

        if(this.path.toLowerCase().endsWith(".zip")) {
            try {
                ZipFile var8 = new ZipFile(this.shadowSelf);
                this.shadowZIP = var8;
                this.setType(SmartFile.SmartType.ZIPFILE);
            } catch (ZipException var6) {
                var6.printStackTrace();
            } catch (IOException var7) {
                var7.printStackTrace();
            }
        }

    }

    private boolean subpathExistsT64(String var1, String var2) {
        Iterator var4 = this.shadowT64.getDir().iterator();

        boolean var3;
        while(true) {
            if(!var4.hasNext()) {
                var3 = false;
                break;
            }

            if(((T64Format.DirEntry)var4.next()).getFilename().replaceAll("[ ]+$", "").equals(var2)) {
                var3 = true;
                break;
            }
        }

        return var3;
    }

    private boolean subpathExistsZIP(String var1, String var2) {
        boolean var3;
        if(this.shadowZIP.getEntry(var2) != null) {
            var3 = true;
        } else {
            var3 = false;
        }

        return var3;
    }

    public int byteAt(long var1) {
        byte var4 = -1;
        int var3;
        if(this.getType() == SmartFile.SmartType.PHYSICAL) {
            if(this.shadowFileReader == null) {
                try {
                    this.shadowFileReader = new byte[(int)this.length()];
                    FileInputStream var5 = new FileInputStream(this.shadowSelf);
                    var5.read(this.shadowFileReader);
                    var5.close();
                    System.err.println("read it");
                } catch (FileNotFoundException var6) {
                    System.err.println("not found: " + this.getAbsolutePath());
                    var3 = var4;
                    return var3;
                } catch (IOException var7) {
                    System.err.println("i/o: " + this.getAbsolutePath());
                    var3 = var4;
                    return var3;
                }
            }

            if(var1 < (long)this.shadowFileReader.length && var1 >= 0L) {
                var3 = this.shadowFileReader[(int)var1] & 255;
            } else {
                System.err.println("out of range seek: " + this.getAbsolutePath());
                var3 = var4;
            }
        } else if(this.getType() == SmartFile.SmartType.T64FILE) {
            if(this.shadowT64Entry == null) {
                this.shadowT64Entry = this.entryT64(this.path, this.subpath);
                var3 = var4;
                if(this.shadowT64Entry == null) {
                    return var3;
                }
            }

            if(var1 == 0L) {
                var3 = this.shadowT64Entry.getStart() % 256;
            } else if(var1 == 1L) {
                var3 = this.shadowT64Entry.getStart() / 256;
            } else {
                byte[] var8 = this.shadowT64Entry.getProgramData();
                var3 = var4;
                if(var1 - 2L < (long)var8.length) {
                    var3 = var4;
                    if(var1 - 2L >= 0L) {
                        var3 = var8[(int)(var1 - 2L)] & 255;
                    }
                }
            }
        } else {
            var3 = var4;
            if(this.getType() == SmartFile.SmartType.ZIPFILE) {
                if(this.shadowFileReader == null) {
                    this.shadowFileReader = this.decompressFile();
                }

                if(var1 < (long)this.shadowFileReader.length && var1 >= 0L) {
                    var3 = this.shadowFileReader[(int)var1] & 255;
                } else {
                    System.err.println("out of range seek: " + this.getAbsolutePath());
                    var3 = var4;
                }
            }
        }

        return var3;
    }

    public byte[] decompressFile()
    {
        try
        {
            ZipInputStream localZipInputStream = new ZipInputStream(new FileInputStream(this.path));
            ZipEntry localZipEntry;
            for (Object localObject = localZipInputStream.getNextEntry(); ; localObject = localZipEntry)
            {
                byte[] arrayOfByte;
                if (localObject == null)
                {
                    localZipInputStream.close();
                    arrayOfByte = new byte[0];
                    return arrayOfByte;
                }
                while (true)
                {
                    if (!((ZipEntry)localObject).getName().equals(this.subpath))
                        break;
                    arrayOfByte = extractFile(localZipInputStream);
                    localZipInputStream.closeEntry();
                    localZipInputStream.close();
                    return arrayOfByte;
                }
                localZipInputStream.closeEntry();
                localZipEntry = localZipInputStream.getNextEntry();
            }
        }
        catch (IOException localIOException)
        {
            return new byte[0];
        }

    }

    public boolean exists() {
        boolean var1;
        if(this.subpath.length() == 0) {
            var1 = this.shadowSelf.exists();
        } else if(!this.shadowSelf.exists()) {
            var1 = false;
        } else if(this.getType() == SmartFile.SmartType.T64FILE) {
            var1 = this.subpathExistsT64(this.path, this.subpath);
        } else if(this.getType() == SmartFile.SmartType.ZIPFILE) {
            var1 = this.subpathExistsZIP(this.path, this.subpath);
        } else {
            var1 = true;
        }

        return var1;
    }

    public byte[] getBuffer() {
        byte[] var2;
        if(this.getType() == SmartFile.SmartType.PHYSICAL) {
            try {
                var2 = new byte[(int)this.length()];
                FileInputStream var3 = new FileInputStream(this.shadowSelf);
                var3.read(var2, 0, var2.length);
                var3.close();
            } catch (FileNotFoundException var5) {
                var2 = null;
            } catch (IOException var6) {
                var2 = null;
            }
        } else if(this.getType() == SmartFile.SmartType.T64FILE) {
            if(this.shadowT64Entry == null) {
                this.shadowT64Entry = this.entryT64(this.path, this.subpath);
                if(this.shadowT64Entry == null) {
                    var2 = null;
                    return var2;
                }
            }

            byte[] var4 = this.shadowT64Entry.getProgramData();
            byte[] var7 = new byte[var4.length + 2];
            var7[0] = (byte)(this.shadowT64Entry.getStart() % 256);
            var7[1] = (byte)(this.shadowT64Entry.getStart() / 256);
            int var1 = 0;

            while(true) {
                var2 = var7;
                if(var1 >= var4.length) {
                    break;
                }

                var7[var1 + 2] = var4[var1];
                ++var1;
            }
        } else if(this.getType() == SmartFile.SmartType.ZIPFILE) {
            var2 = this.decompressFile();
            System.out.println("Buffer from decompressFile() is " + var2.length + " bytes ");
        } else {
            var2 = null;
        }

        return var2;
    }

    public SmartFile getParentFile() {
        return new SmartFile(this.getParent());
    }

    public SmartFile.SmartType getType() {
        return this.type;
    }

    public boolean isDirectory() {
        boolean var2 = true;
        boolean var1;
        if(this.getType() == SmartFile.SmartType.PHYSICAL && this.shadowSelf.isDirectory()) {
            var1 = var2;
        } else {
            if(this.getType() == SmartFile.SmartType.T64FILE) {
                var1 = var2;
                if(this.subpath.equals("")) {
                    return var1;
                }
            }

            if(this.getType() == SmartFile.SmartType.ZIPFILE) {
                var1 = var2;
                if(this.subpath.length() != 0) {
                    ZipEntry var3 = this.shadowZIP.getEntry(this.subpath);
                    if(var3 == null) {
                        var1 = false;
                    } else {
                        System.err.println("exists: " + var3.getName() + " " + var3.getSize());
                        var1 = var2;
                        if(var3.getSize() != 0L) {
                            var1 = var3.isDirectory();
                        }
                    }
                }
            } else {
                var1 = false;
            }
        }

        return var1;
    }

    public boolean isFile() {
        boolean var1;
        if(this.exists() && !this.isDirectory()) {
            var1 = true;
        } else {
            var1 = false;
        }

        return var1;
    }

    public long length() {
        long var1 = 0L;
        if(this.exists()) {
            if(this.subpath.length() == 0) {
                var1 = super.length();
            } else if(this.getType() == SmartFile.SmartType.T64FILE) {
                var1 = this.sizeT64();
            } else if(this.getType() == SmartFile.SmartType.ZIPFILE) {
                var1 = this.sizeZIP();
            }
        }

        return var1;
    }

    public SmartFile[] listFiles() {
        SmartFile[] var3;
        if(!this.isDirectory()) {
            var3 = null;
        } else {
            int var1;
            SmartFile[] var4;
            if(this.shadowSelf.isDirectory()) {
                File[] var5 = this.shadowSelf.listFiles();
                var4 = new SmartFile[var5.length];
                var1 = 0;

                while(true) {
                    var3 = var4;
                    if(var1 >= var5.length) {
                        break;
                    }

                    var4[var1] = new SmartFile(var5[var1], "");
                    ++var1;
                }
            } else if(this.getType() == SmartFile.SmartType.T64FILE) {
                ArrayList var7 = this.shadowT64.getDir();
                var4 = new SmartFile[var7.size()];
                var1 = 0;

                while(true) {
                    var3 = var4;
                    if(var1 >= var7.size()) {
                        break;
                    }

                    var4[var1] = new SmartFile(this.path + File.separator + ((T64Format.DirEntry)var7.get(var1)).getFilename().replaceAll("[ ]+$", ""));
                    ++var1;
                }
            } else if(this.getType() == SmartFile.SmartType.ZIPFILE) {
                Enumeration var8 = this.shadowZIP.entries();
                var4 = new SmartFile[this.shadowZIP.size()];
                var1 = 0;

                while(true) {
                    boolean var2;
                    ZipEntry var6;
                    do {
                        if(!var8.hasMoreElements()) {
                            var3 = var4;
                            if(var1 < var4.length) {
                                var3 = (SmartFile[])Arrays.copyOfRange(var4, 0, var1);
                            }

                            return var3;
                        }

                        var6 = (ZipEntry)var8.nextElement();
                        if(!var6.isDirectory() && var6.getSize() != 0L) {
                            var2 = false;
                        } else {
                            var2 = true;
                        }
                    } while(!var6.getName().startsWith(this.subpath + File.separator) && (this.subpath.length() != 0 || var6.getName().contains(File.separator) && (!var2 || var6.getName().indexOf(File.separator) != var6.getName().length() - 1)));

                    if(!var6.getName().equals(this.subpath + File.separator) && !var6.getName().startsWith("_") && !var6.getName().endsWith(".DS_Store")) {
                        var4[var1] = new SmartFile(this.path + File.separator + this.subpath, var6.getName());
                        ++var1;
                    }
                }
            } else {
                var3 = new SmartFile[0];
            }
        }

        return var3;
    }

    public void setType(SmartFile.SmartType var1) {
        this.type = var1;
    }

    public static enum SmartType {
        PHYSICAL,
        T64FILE,
        ZIPFILE;
    }
}
