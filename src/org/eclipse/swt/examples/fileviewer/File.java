package org.eclipse.swt.examples.fileviewer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import lrscp.lib.Log;

import org.eclipse.swt.examples.fileviewer.FileListingService.FileEntry;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.android.ddmlib.SyncException;
import com.android.ddmlib.SyncException.SyncError;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.SyncService.ISyncProgressMonitor;
import com.android.ddmlib.TimeoutException;
import com.android.ddmuilib.SyncProgressHelper;
import com.android.ddmuilib.SyncProgressHelper.SyncRunnable;

public class File implements Comparable<File> {
    public static final char separator = '/';
    public static FileListingService sv = new FileListingService(null);

    public static ArrayList<FileEntry> cachedEntries = new ArrayList<FileEntry>();

    FileEntry mEntry = null;
    private static String rootPath = "/";

    public File(String path) {
        if (path == null) {
            throw new NullPointerException();
        }
        mEntry = getCachedEntry(path);
        if (mEntry != null) {
            return;
        }
        String parent = FileEntry.getParent(path);
        if (parent == null) {
            mEntry = sv.getRoot();
            setCachedEntry(mEntry);
        } else {
            mEntry = new FileEntry(parent);
            sv.getChildren(mEntry, true, null);
            setCachedEntry(mEntry);
            mEntry = mEntry.findChild(FileEntry.getBaseName(path));
            setCachedEntry(mEntry);
        }
    }

    FileEntry getCachedEntry(String path) {
        if (path == null) {
            return null;
        }
        // Log.i("find cache:" + path);
        int depth = getPathDepth(path);
        if (cachedEntries.size() < depth) {
            return null;
        }

        FileEntry entry = cachedEntries.get(depth - 1);
        if (entry != null && entry.path.equals(path)) {
            // Log.i("get cached:" + entry.path);
            return entry;
        }

        return null;
    }

    void setCachedEntry(FileEntry entry) {
        // Log.i("set cached:" + entry.path);
        if (entry == null) {
            return;
        }
        int depth = getPathDepth(entry.path);
        while (cachedEntries.size() < depth) {
            cachedEntries.add(null);
        }
        cachedEntries.set(depth - 1, entry);
        for (FileEntry e : cachedEntries) {
            // Log.i("All Entrys:" + e.path);
        }
    }

    int getPathDepth(String path) {
        int i = 1;
        while ((path = FileEntry.getParent(path)) != null) {
            i++;
        }
        return i;
    }

    public File(FileEntry entry) {
        if (entry == null) {
            throw new NullPointerException();
        }
        this.mEntry = entry;
    }

    public File(File targetFile, String name) {
        // sv.getChildren(mEntry, false, null);
    }

    public File(char separator2) {
        mEntry = sv.getRoot();
        sv.getChildren(mEntry, true, null);
    }

    public String getAbsolutePath() {
        return mEntry != null ? mEntry.getFullPath() : "";
    }

    public boolean isLink() {
        return mEntry != null ? mEntry.getType() == FileListingService.TYPE_LINK : false;
    }

    public boolean isDirectory() {
        return mEntry != null ? mEntry.isDirectory() : false;
    }

    public String getPath() {
        return mEntry != null ? mEntry.getFullPath() : "";
    }

    public String getName() {
        return mEntry != null ? mEntry.getName() : "";
    }

    public File getParentFile() {
        String parent = FileEntry.getParent(mEntry.path);
        Log.i(parent);
        try {
            if (parent == null) {
                return null;
            } else {
                return new File(parent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int length() {
        try {
            return mEntry != null ? mEntry.getSizeValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean exists() {
        return mEntry != null ? true : false;
    }

    public File[] listFiles() {
        // Log.i("listFile:" + getPath());
        sv.getChildren(mEntry, true, null);
        FileEntry[] entries = mEntry.getCachedChildren();
        File[] files = new File[entries.length];
        for (int i = 0; i < entries.length; i++) {
            files[i] = new File(entries[i]);
        }
        return files;
    }

    public boolean mkdirs() {
        return false;
    }

    public InputStream getInputStream() {
        return null;
    }

    public OutputStream getOutputStream() {
        return null;
    }

    public boolean delete() {
        StringBuilder msg = new StringBuilder();
        if (sv.delete(mEntry, msg) != 0) {
            MessageBox msgBox = new MessageBox(new Shell());
            msgBox.setMessage(msg.toString());
            msgBox.setText("error");
            msgBox.open();
            return false;
        }
        return true;
    }

    public String lastModified() {
        return mEntry != null ? mEntry.getDate() : "";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof File) {
            return ((File) obj).getPath().equals(getPath());
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nEntry:");
        sb.append("\n  name:" + mEntry.getName());
        sb.append("\n  FullPath:" + mEntry.getFullPath());
        sb.append("\n  FullEscapedPath:" + mEntry.getFullEscapedPath());
        sb.append("\n  Size:" + mEntry.getSize());
        sb.append("\n  Date:" + mEntry.getDate());
        return sb.toString();
    }

    /**
     * Pulls a file from a device.
     * 
     * @param remote
     *            the remote file on the device
     * @param local
     *            the destination filepath
     */
    public void pull(final String local, Shell parent, final Runnable callBack) {
        final SyncService sync;

        try {
            sync = sv.mDevice.getSyncService();

            if (sync != null) {
                SyncProgressHelper.run(new SyncRunnable() {
                    @Override
                    public void run(ISyncProgressMonitor monitor) throws SyncException,
                            IOException, TimeoutException {
                        sync.pullFile(getPath(), mEntry.getSizeValue(), local, monitor);
                    }

                    @Override
                    public void close() {
                        sync.close();
                        if (callBack != null) {
                            callBack.run();
                        }
                    }
                }, String.format("Pulling %1$s from the device", getName()), parent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageBox msg = new MessageBox(parent);
            if (e.getMessage() != null) {
                msg.setMessage(e.getMessage());
            }
            msg.setText("Error!!");
            msg.open();
        }
    }

    public void pullFiles(final File[] files, final String local, Shell parent,
            final Runnable callBack) {
        try {
            final SyncService sync = sv.mDevice.getSyncService();
            if (sync != null) {
                SyncProgressHelper.run(new SyncRunnable() {
                    @Override
                    public void run(ISyncProgressMonitor monitor) throws SyncException,
                            IOException, TimeoutException {
                        monitor.start(getTotalRemoteFileSize(files));
                        doPullFiles(files, local, monitor, sync);
                        monitor.stop();
                    }

                    @Override
                    public void close() {
                        sync.close();
                        if (callBack != null) {
                            callBack.run();
                        }
                    }
                }, String.format("Pulling %1$s from the device", getName()), parent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageBox msg = new MessageBox(parent);
            if (e.getMessage() != null) {
                msg.setMessage(e.getMessage());
            }
            msg.setText("Error!!");
            msg.open();
        }
    }

    public void pushFiles(Shell parent, final String[] local, final File remote,
            final Runnable callBack) {
        try {
            final SyncService sync = sv.mDevice.getSyncService();
            if (sync != null) {
                SyncProgressHelper.run(new SyncRunnable() {
                    @Override
                    public void run(ISyncProgressMonitor monitor) throws SyncException,
                            IOException, TimeoutException {
                        if (remote.isDirectory() == false) {
                            throw new SyncException(SyncError.REMOTE_IS_FILE);
                        }

                        // make a list of File from the list of String
                        ArrayList<java.io.File> files = new ArrayList<java.io.File>();
                        for (String path : local) {
                            files.add(new java.io.File(path));
                        }

                        // get the total count of the bytes to transfer
                        java.io.File[] fileArray = files.toArray(new java.io.File[files.size()]);
                        int total = sync.getTotalLocalFileSize(fileArray);

                        monitor.start(total);

                        sync.doPush(fileArray, remote.getPath(), monitor);

                        monitor.stop();
                    }

                    @Override
                    public void close() {
                        sync.close();
                        if (callBack != null) {
                            callBack.run();
                        }
                    }
                }, String.format("Push to the device %1$s", getName()), parent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            notifyError(e.getMessage());
        }
    }

    void notifyError(String msg) {
        MessageBox msgBox = new MessageBox(new Shell());
        if (msg != null)
            msgBox.setMessage(msg);
        msgBox.setText("Error!!");
        msgBox.open();
    }

    private int getTotalRemoteFileSize(File[] entries) {
        int count = 0;
        for (File e : entries) {
            FileEntry entry = e.mEntry;
            int type = entry.getType();
            if (type == FileListingService.TYPE_DIRECTORY) {
                // get the children
                File[] children = e.listFiles();
                count += getTotalRemoteFileSize(children) + 1;
            } else if (type == FileListingService.TYPE_FILE) {
                count += entry.getSizeValue();
            }
        }

        return count;
    }

    private void doPullFiles(final File[] files, String localPath, ISyncProgressMonitor monitor,
            SyncService sync) throws SyncException, IOException, TimeoutException {
        for (File e : files) {
            FileEntry entry = e.mEntry;
            // check if we're cancelled
            if (monitor.isCanceled() == true) {
                throw new SyncException(SyncError.CANCELED);
            }

            // get type (we only pull directory and files for now)
            int type = entry.type;
            if (type == FileListingService.TYPE_DIRECTORY) {
                monitor.startSubTask(entry.path);
                String dest = localPath + File.separator + e.getName();

                // make the directory
                java.io.File d = new java.io.File(dest);
                d.mkdir();

                // then recursively call the content. Since we did a ls command
                // to get the number of files, we can use the cache
                File[] children = e.listFiles();
                doPullFiles(children, dest, monitor, sync);
                monitor.advance(1);
            } else if (type == FileListingService.TYPE_FILE) {
                monitor.startSubTask(entry.path);
                String dest = localPath + File.separator + e.getName();
                sync.doPullFile(entry.path, dest, monitor);
            }
        }
    }

    @Override
    public int compareTo(File o) {
        return getPath().compareTo(o.getPath());
    }

    public void copyTo(File directory) {
        StringBuilder msg = new StringBuilder();
        if (sv.copyTo(mEntry, directory.mEntry, msg) != 0) {
            notifyError(msg.toString());
        }
    }

    public void moveTo(File directory) {
        StringBuilder msg = new StringBuilder();
        if (sv.moveTo(mEntry, directory.mEntry, msg) != 0) {
            notifyError(msg.toString());
        }
    }

    /*
     * public static void main(String[] args) { AndroidDebugBridge.init(true);
     * AndroidDebugBridge.createBridge("adb", true);
     * AndroidDebugBridge.addDeviceChangeListener(new IDeviceChangeListener() {
     * 
     * @Override public void deviceDisconnected(IDevice device) { // TODO
     * Auto-generated method stub
     * 
     * }
     * 
     * @Override public void deviceConnected(IDevice device) { sv = new
     * FileListingService(device); Log.i(new File("/mnt").length()); }
     * 
     * @Override public void deviceChanged(IDevice device, int changeMask) { //
     * TODO Auto-generated method stub
     * 
     * } }); }
     */
}
