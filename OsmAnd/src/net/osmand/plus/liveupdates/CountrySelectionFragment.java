package net.osmand.plus.liveupdates;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CountrySelectionFragment extends BaseOsmAndDialogFragment {

	private List<CountryItem> countryItems = new ArrayList<>();
	private OnFragmentInteractionListener mListener;

	public List<CountryItem> getCountryItems() {
		return countryItems;
	}

	public CountryItem getCountryItem(String downloadName) {
		if (!Algorithms.isEmpty(downloadName)) {
			for (CountryItem item : countryItems) {
				if (downloadName.equals(item.downloadName)) {
					return item;
				}
			}
		}
		return null;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		if (countryItems.size() == 0) {
			initCountries(getMyApplication());
		}

		View view = inflater.inflate(R.layout.fragment_search_list, container, false);
		ListView listView = (ListView) view.findViewById(android.R.id.list);
		final ArrayAdapter<CountryItem> adapter = new ListAdapter(getListItemIcon());
		if (countryItems.size() > 0) {
			adapter.addAll(countryItems);
		}
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mListener.onSearchResult(adapter.getItem(position));
				dismiss();
			}
		});
		final EditText searchEditText = (EditText) view.findViewById(R.id.searchEditText);
		searchEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				adapter.getFilter().filter(s);
			}
		});
		ImageButton clearButton = (ImageButton) view.findViewById(R.id.clearButton);
		setThemedDrawable(clearButton, R.drawable.ic_action_remove_dark);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		return view;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnFragmentInteractionListener) {
			mListener = (OnFragmentInteractionListener) context;
		} else if (getParentFragment() instanceof OnFragmentInteractionListener) {
			mListener = (OnFragmentInteractionListener) getParentFragment();
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@DrawableRes
	protected int getListItemIcon() {
		return R.drawable.ic_map;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	public interface OnFragmentInteractionListener {
		void onSearchResult(CountryItem name);
	}


	public void initCountries(OsmandApplication app) {
		final WorldRegion root = app.getRegions().getWorldRegion();
		ArrayList<WorldRegion> groups = new ArrayList<>();
		groups.add(root);
		processGroup(root, groups);
		Collections.sort(groups, new Comparator<WorldRegion>() {
			@Override
			public int compare(WorldRegion lhs, WorldRegion rhs) {
				if (lhs == root) {
					return -1;
				}
				if (rhs == root) {
					return 1;
				}
				return getHumanReadableName(lhs).compareTo(getHumanReadableName(rhs));
			}
		});
		for (WorldRegion group : groups) {
			String name = getHumanReadableName(group);
			countryItems.add(new CountryItem(name, group.getRegionDownloadName()));
		}
	}

	private static void processGroup(WorldRegion group,
									 List<WorldRegion> nameList) {
		if (group.isRegionMapDownload()) {
			nameList.add(group);
		}

		if (group.getSubregions() != null) {
			for (WorldRegion g : group.getSubregions()) {
				processGroup(g, nameList);
			}
		}
	}

	private static String getHumanReadableName(WorldRegion group) {
		String name;
		if (group.getLevel() > 2 || (group.getLevel() == 2
				&& group.getSuperregion().getRegionId().equals(WorldRegion.RUSSIA_REGION_ID))) {
			WorldRegion parent = group.getSuperregion();
			WorldRegion parentsParent = group.getSuperregion().getSuperregion();
			if (group.getLevel() == 3) {
				if (parentsParent.getRegionId().equals(WorldRegion.RUSSIA_REGION_ID)) {
					name = parentsParent.getLocaleName() + " " + group.getLocaleName();
				} else if (!parent.getRegionId().equals(WorldRegion.UNITED_KINGDOM_REGION_ID)) {
					name = parent.getLocaleName() + " " + group.getLocaleName();
				} else {
					name = group.getLocaleName();
				}
			} else {
				name = parent.getLocaleName() + " " + group.getLocaleName();
			}
		} else {
			name = group.getLocaleName();
		}
		if (name == null) {
			name = "";
		}
		return name;
	}

	public static class CountryItem {
		private String localName;
		private String downloadName;

		public CountryItem(String localName, String downloadName) {
			this.localName = localName;
			this.downloadName = downloadName;
		}

		public String getLocalName() {
			return localName;
		}

		public String getDownloadName() {
			return downloadName;
		}

		@Override
		public String toString() {
			return localName;
		}
	}

	private class ListAdapter extends ArrayAdapter<CountryItem> {
		private final Drawable drawableLeft;

		public ListAdapter(@DrawableRes int drawableLeftId) {
			super(getMyActivity(), android.R.layout.simple_list_item_1);
			this.drawableLeft = drawableLeftId == -1 ? null : getContentIcon(drawableLeftId);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			CountryItem item = getItem(position);
			TextView view = (TextView) super.getView(position, convertView, parent);
			view.setText(item.localName);
			view.setCompoundDrawablesWithIntrinsicBounds(drawableLeft, null, null, null);
			view.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.list_content_padding));
			return view;
		}
	}
}
