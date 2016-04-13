package de.tu_berlin.snet.cellactivity;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.tu_berlin.snet.cellactivity.util.CellInfo;
import de.tu_berlin.snet.cellactivity.util.Event;
import de.tu_berlin.snet.cellactivity.util.EventList;

/**
 * Created by ashish on 21.03.16.
 */
public class LocationGoogleApiAsync extends AsyncTask<Void,Void,String> {

    public interface HiddenApiTaskListener {
        void onLocationReceived(Location HiddenApiLocation, int key);
    }
    private Event event;
    private  int key;
    private Location hiddenApiLocation;
    private final HiddenApiTaskListener taskListener;
    public LocationGoogleApiAsync(HiddenApiTaskListener tasklistener, Event event, int key){
        this.event = event;
        this.key = key;
        this.taskListener = tasklistener;
        this.hiddenApiLocation = new Location("hiddenApi");
    }
    protected void onPreExecute() {
        //display progress dialog.
    }
    protected String doInBackground(Void... params) {

       String ret = "0,0";
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
            WriteData(outputStream, event.cellinfo.getCellId(), event.cellinfo.getLac());
            Log.d("Async URL", "("+ event.cellinfo.getCellId()+" " +event.cellinfo.getLac()+")");
            InputStream inputStream = urlConnection.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            dataInputStream.readShort();
            dataInputStream.readByte();
            int code = dataInputStream.readInt();
            if (code == 0) {
                Log.d("Async URL","query success");
                double lat = (double) dataInputStream.readInt() / 1000000D;
                double lng = (double) dataInputStream.readInt() / 1000000D;
                int accuracy = dataInputStream.readInt();
                dataInputStream.readInt();
                dataInputStream.readUTF();
                this.hiddenApiLocation.setLatitude(lat);
                this.hiddenApiLocation.setLongitude(lng);
                this.hiddenApiLocation.setAccuracy(accuracy);
                ret = Double.toString(lat) + "," + Double.toString(lng);
            }
            else {
                Log.d("Async URL", "query failed");
                //hiddenApiLocation=null;
            }
        }catch (Exception e){
            ret = e.toString();
        }
        return ret;
    }
    protected void onPostExecute(String result) {
        // dismiss progress dialog and update ui
        Log.d("Async Task", result);
        if(this.taskListener != null) {
            EventList.getmInstance().eventMap.get(key).endtimestamp = System.currentTimeMillis();
            this.taskListener.onLocationReceived(hiddenApiLocation, this.key);
        }
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