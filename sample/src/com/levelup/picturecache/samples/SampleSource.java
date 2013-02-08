package com.levelup.picturecache.samples;

import java.util.ArrayList;
import java.util.List;

public class SampleSource {

	private final static ArrayList<Sample> elements = new ArrayList<Sample>();

	static List<Sample> getSamples() {
		if (elements.isEmpty()) {
			elements.add(new Sample("LevelUp", "https://si0.twimg.com/profile_images/1584807085/LevelUp-Logo-Avatarv5.png"));
			elements.add(new Sample("PlumeApp", "https://si0.twimg.com/profile_images/2794557097/a073ce673f72cb8fc1aa189ad7f28fd6.png"));
			elements.add(new Sample("robUx4", "https://si0.twimg.com/profile_images/1298570155/R-612702-1288512541.jpeg"));
			elements.add(new Sample("MatroskaOrg", "https://si0.twimg.com/profile_images/1187770441/avatar.png"));
			elements.add(new Sample("invalid url", ""));
			elements.add(new Sample("Android", "https://si0.twimg.com/profile_images/3092003750/9b72a46e957a52740c667f4c64fa5d10.jpeg"));
			elements.add(new Sample("RomainGuy", "https://si0.twimg.com/profile_images/1670809802/Video_making.jpg"));
			elements.add(new Sample("ChrisBanes", "https://si0.twimg.com/profile_images/2415858898/kiujjwj2pxd9rj8j6ofi.jpeg"));
			elements.add(new Sample("JakeWharton", "https://si0.twimg.com/profile_images/2233096055/Untitled2.png"));
			elements.add(new Sample("LudovicVialle", "https://si0.twimg.com/profile_images/1340116626/75882_1404050316228_1681490550_783708_4299498_n.jpg"));
			elements.add(new Sample("SlidingMenu", "https://si0.twimg.com/profile_images/2809065445/a251373b9ca377dab64ff0e7fc3c6870.png"));
			elements.add(new Sample("Google", "https://si0.twimg.com/profile_images/2504370963/6u5qf6cl9jtwew6poxcj.png"));
			elements.add(new Sample("Twitter", "https://si0.twimg.com/profile_images/2284174758/v65oai7fxn47qv9nectx.png"));
			elements.add(new Sample("Facebook", "https://si0.twimg.com/profile_images/81302971/facebook_favicon_large_2.png"));
			elements.add(new Sample("JulienDelRio", "https://si0.twimg.com/profile_images/736723302/19037_1316751116208_1153635956_978327_7408179_n.jpg"));
			elements.add(new Sample("PomepuyN", "https://si0.twimg.com/profile_images/1640117577/PomepuyNw_346.jpg"));
			elements.add(new Sample("RomMout", "https://si0.twimg.com/profile_images/2377111206/dvil.png"));
			elements.add(new Sample("GLoupiac", "https://si0.twimg.com/sticky/default_profile_images/default_profile_1.png"));
		}
		return elements;
	}
	
	protected static class Sample {
		final String picURL;
		final String name;

		Sample(String name, String pic) {
			this.picURL = pic;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
	
}
