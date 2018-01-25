package com.example.android.mywallet;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    /**
     * URL to query the number of most recent block
     */
    private static final String ETHER_REQUEST_URL = "https://api.etherscan.io/api?module=proxy&action=eth_blockNumber&apikey=YourApiKeyToken";

    private ListView transactionListView;

    ArrayList<HashMap<String, String>> transactionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        transactionListView = (ListView) findViewById(R.id.list_view);

        transactionList = new ArrayList<>();

        // Kick off an {@link AsyncTask} to perform the network request
        EtherAsyncTask task = new EtherAsyncTask();
        task.execute();
    }

    private class EtherAsyncTask extends AsyncTask<URL, Void, Void> {

        @Override
        protected Void doInBackground(URL... urls) {

            // Create URL object
            URL url = createUrl(ETHER_REQUEST_URL);

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem making the HTTP request.", e);
            }


            if (jsonResponse != null) {
                try {
                    // Create a new JSONObject
                    JSONObject jsonObj = new JSONObject(jsonResponse);

                    // Get the JSON Array node
                    JSONArray transactions = jsonObj.getJSONArray("transactions");

                    // Looping through all transactions
                    for (int i = 0; i < transactions.length(); i++) {
                        // Get the JSONObject
                        JSONObject transObject = transactions.getJSONObject(i);
                        String blockHash = transObject.getString("blockHash");
                        String blockNumber = transObject.getString("blockNumber");
                        String from = transObject.getString("from");

                        // Hash map for a single transaction
                        HashMap<String, String> transaction = new HashMap<>();

                        // Add each child node to HashMap key => value
                        transaction.put("blockHash", blockHash);
                        transaction.put("blockNumber", blockNumber);
                        transaction.put("from", from);

                        // Adding a transactions to our transaction list
                        transactionList.add(transaction);
                    }

                } catch (final JSONException e) {
                Log.e(LOG_TAG, "Json parsing error: " + e.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Json parsing error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });

            }

                 } else {
                     Log.e(LOG_TAG, "Couldn't get json from server.");
                      runOnUiThread(new Runnable() {
                      @Override
                     public void run() {
                          Toast.makeText(getApplicationContext(),
                            "Couldn't get json from server.",
                            Toast.LENGTH_LONG).show();
                        }
                      });
                 }
            return null;
       }




    /**
         * Update the screen with the given block (which was the result of the
         * {@link EtherAsyncTask}).
         */
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        ListAdapter adapter = new SimpleAdapter(MainActivity.this, transactionList,
                R.layout.list_item, new String[]{"blockHash", "blockNumber", "from"},
                new int[]{R.id.blockHash, R.id.blockNumber, R.id.from});
        transactionListView.setAdapter(adapter);
    }


        /**
         * Returns new URL object from the given string URL.
         */
        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Error with creating URL", exception);
                return null;
            }
            return url;
        }

        /**
         * Make an HTTP request to the given URL and return a String as the response.
         */
        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";

            // If the URL is null, then return early.
            if (url == null) {
                return jsonResponse;
            }

            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();

                // If the request was successful (response code 200),
                // then read the input stream and parse the response.
                if (urlConnection.getResponseCode() == 200) {
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                } else {
                    Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem retrieving the ether JSON results.", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    // function must handle java.io.IOException here
                    inputStream.close();
                }
            }
            return jsonResponse;

        }

        /**
         * Convert the {@link InputStream} into a String which contains the
         * whole JSON response from the server.
         */
        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

    }
}
