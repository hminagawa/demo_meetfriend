package com.appiaries.meetfriend.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

import android.content.Context;

/**
 * Generate an unique ID within the app.
 *
 * @author yoshihide-sogawa
 * @see <a href="http://android-developers.blogspot.jp/2011/03/identifying-app-installations.html">Link</a>
 */
public final class Installation {

    /** Unique ID */
    private static String sID = null;
    /** File name */
    private static final String FILE_NAME = "meetfriend_uuid";

    /** Encapsulate the constructor */
    private Installation() {
    }

    /**
     * Retrieve the UUID.
     * 
     * @param context
     *            {@link Context}
     * @return UUID
     */
    public synchronized static String id(final Context context) {
        if (sID == null) {
            final File installation = new File(context.getFilesDir(), FILE_NAME);
            try {
                if (!installation.exists()) {
                    writeInstallationFile(installation);
                }
                sID = readInstallationFile(installation);
            } catch (IOException e) {
                // No procedures.
            }
        }
        return sID;
    }

    /**
     * Generate ID for this demo.
     * No hyphens allowed (for 30 chars restriction).
     *
     * @param context
     *            {@link Context}
     * @return UUID
     */
    public synchronized static String id4Ap(final Context context) {
        return id(context).replaceAll("-", "").substring(0, 30);
    }

    /**
     * Retrieve UUID from the file.
     *
     * @param installation
     *            {@link File}
     * @return UUID
     * @throws IOException
     */
    private static String readInstallationFile(final File installation) throws IOException {
        final RandomAccessFile f = new RandomAccessFile(installation, "r");
        final byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes, "UTF-8");
    }

    /**
     * Write the UUID.
     * 
     * @param installation
     *            {@link File}
     * @throws IOException
     */
    private static void writeInstallationFile(final File installation) throws IOException {
        final FileOutputStream out = new FileOutputStream(installation);
        final String id = UUID.randomUUID().toString();
        out.write(id.getBytes("UTF-8"));
        out.close();
    }
}
