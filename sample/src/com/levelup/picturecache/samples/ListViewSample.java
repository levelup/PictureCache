package com.levelup.picturecache.samples;



import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class ListViewSample extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setListAdapter(new AvatarsAdapter(this));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.list_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.alternativeLoader) {
			if (item.isChecked()) {
				// back to the normal loaded
				setListAdapter(new AvatarsAdapter(this));
				item.setChecked(false);
			} else {
				// use the alternate loader
				setListAdapter(new AlternateAvatarsAdapter(this));
				item.setChecked(true);
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
