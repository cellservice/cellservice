package de.tu_berlin.snet.cellservice;

import android.location.Location;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

import de.tu_berlin.snet.cellservice.model.record.CellInfo;

/**
 * Created by ashish on 21.03.16.
 */
public class LocationGoogleApiFuture implements Callable<Location>{
    private static final String LOG_TAG = LocationGoogleApiFuture.class.getSimpleName();

    private Location hiddenApiLocation = null;
    private CellInfo cellInfo;
    public  LocationGoogleApiFuture (CellInfo cellInfo){
        //adding custom provider name to location as hiddenApi
        this.cellInfo = cellInfo;
    }

    @Override
    public Location call() throws Exception {
        try {
            URL url = new URL("http://www.google.com/glm/mmap");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            Log.d("Async URL", url.toString());
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            // urlConnection.setChunkedStreamingMode(0);
            urlConnection.connect();

            OutputStream outputStream = urlConnection.getOutputStream();
            WriteData(outputStream, cellInfo.getCellId(), cellInfo.getLac());
            Log.d("Async URL", "("+ cellInfo.getCellId()+" " +cellInfo.getLac()+")");
            InputStream inputStream = urlConnection.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            dataInputStream.readShort();
            dataInputStream.readByte();
            int code = dataInputStream.readInt();
            if (code == 0) {
                Log.d("Async URL","query success");
                this.hiddenApiLocation = new Location("hiddenApi");
                double lat = (double) dataInputStream.readInt() / 1000000D;
                double lng = (double) dataInputStream.readInt() / 1000000D;
                int accuracy = dataInputStream.readInt();
                dataInputStream.readInt();
                dataInputStream.readUTF();
                this.hiddenApiLocation.setLatitude(lat);
                this.hiddenApiLocation.setLongitude(lng);
                this.hiddenApiLocation.setAccuracy(accuracy);
                return hiddenApiLocation;
            }
            else {
                Log.d("Async URL", "query failed");
                //hiddenApiLocation=null;
                return hiddenApiLocation;
            }
        } catch (Exception e){
            Log.d(LOG_TAG, e.getMessage());
        }
        return null;
    }

    private void WriteData(OutputStream out,int cellId, int lac)throws
            IOException {
        DataOutputStream os = new DataOutputStream(out);
        os.writeShort(21);
        os.writeLong(0);
        os.writeUTF("fr");
        os.writeUTF("Sony_Ericsson-K750");
        os.writeUTF("1.3.1");
        os.writeUTF("Web");
        os.writeByte(27);
        os.writeInt(0);
        os.writeInt(0);
        os.writeInt(3);
        os.writeUTF("");
        os.writeInt(cellId);  // CELL-ID
        os.writeInt(lac);     // LAC
        os.writeInt(0);
        os.writeInt(0);
        os.writeInt(0);
        os.writeInt(0);
        os.flush();

    }

}