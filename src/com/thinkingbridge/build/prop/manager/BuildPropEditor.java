package com.thinkingbridge.build.prop.manager;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class BuildPropEditor extends ListActivity {
    private ListView listView;
    private String tempFile;
    private boolean refreshList;
	private ActionBar actionBar;
	
	//처음실행될때 경고문이 나옴
	
		@Override
		protected void onStart()
		{
		    super.onStart();

		    final Dialog dialog = new Dialog(this);
		    dialog.setContentView(R.layout.dialog);
		    dialog.setTitle("Warning!");

		    Button button = (Button) dialog.findViewById(R.id.okay);
		    button.setOnClickListener(new OnClickListener() {  
		        @Override  
		        public void onClick(View view) {  
		            dialog.dismiss();            
		        }  
		    });

		    dialog.show();
		}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        actionBar = getActionBar();
        
        listView = getListView();
        listView.setTextFilterEnabled(true);

        createTempFile();
        populateList();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	// TODO: ....
    	if (refreshList) {
    		populateList();
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.add_menu:
    			refreshList = true;
    			showEdit(null, null);
    			break;
    	}
    	return true;
    }
    
    public void populateList() {
    	final Properties prop = new Properties();
        File file = new File(tempFile);
    	try {
    		prop.load(new FileInputStream(file));
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), "Error: " + e, Toast.LENGTH_SHORT).show();
		}
    	
    	final String[] pTitle = prop.keySet().toArray(new String[0]);
    	final List<String> pDesc = new ArrayList<String>();
    	for (int i = 0; i < pTitle.length; i++) {
    		pDesc.add(prop.getProperty(pTitle[i]));
    	}
    	
    	ArrayList<Map<String, String>> list = buildData(pTitle, pDesc);
		String[] from = { "title", "description" };
		int[] to = { R.id.prop_title, R.id.prop_desc };

		SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.list_item, from, to);
		setListAdapter(adapter);
		
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				showEdit(pTitle[position], prop.getProperty(pTitle[position]));
			}
        });
    }
    
    private ArrayList<Map<String, String>> buildData(String[] t, List<String> d) {
		ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();
		
		for (int i = 0; i < t.length; ++i) {
			list.add(putData(t[i], d.get(i)));
		}

		return list;
	}
    
    private HashMap<String, String> putData(String title, String description) {
		HashMap<String, String> item = new HashMap<String, String>();

		item.put("title", title);
		item.put("description", description);

		return item;
	}
    
    public void showEdit(String name, String key) {
    	Intent intent = new Intent(BuildPropEditor.this, EditPropActivity.class);
		
    	intent.putExtra("name", name);
    	intent.putExtra("key", key);
    	
    	startActivity(intent);
    }
    
    private void createTempFile() {
    	Process process = null;
        DataOutputStream os = null;
        
        try {
            process = Runtime.getRuntime().exec("su");
	        os = new DataOutputStream(process.getOutputStream());
	        os.writeBytes("mount -o remount,rw -t yaffs2 /dev/block/mtdblock4 /system\n");
	        os.writeBytes("cp -f /system/build.prop " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/buildprop.tmp\n");
	        os.writeBytes("chmod 777 " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/buildprop.tmp\n");
	        os.writeBytes("exit\n");
	        os.flush();
	        process.waitFor();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            	Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        
        tempFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/buildprop.tmp";
    }
    
    public boolean runRootCommand(String command) {
        Process process = null;
        DataOutputStream os = null;
        
        try {
            process = Runtime.getRuntime().exec("su");
	        os = new DataOutputStream(process.getOutputStream());
	        os.writeBytes(command+"\n");
	        os.writeBytes("exit\n");
	        os.flush();
	        process.waitFor();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            	Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }
}