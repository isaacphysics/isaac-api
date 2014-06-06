package uk.ac.cam.cl.dtg.isaac.models;

import java.util.List;

import uk.ac.cam.cl.dtg.segue.dto.ContentSummary;

import com.google.common.collect.ImmutableList;

public class IndexPage {

	private ImmutableList<IndexPageItem> items;
	private List<ContentSummary> concepts;

	public static class IndexPageItem {
		private String title;
		private String level;
		private String topic;
		private String pdf;
		private boolean contentAvailable;
		
		public IndexPageItem(String title, String level, String topic,
				String pdf, boolean contentAvailable) {
			super();
			this.title = title;
			this.level = level;
			this.topic = topic;
			this.pdf = pdf;
			this.contentAvailable= contentAvailable;
		}

		public boolean isContentAvailable() {
			return contentAvailable;
		}

		public String getTitle() {
			return title;
		}

		public String getLevel() {
			return level;
		}

		public String getTopic() {
			return topic;
		}

		public String getPdf() {
			return pdf;
		}

	}

	public IndexPage(ImmutableList<IndexPageItem> items) {
		this.items = items;
	}

	public IndexPage(List<ContentSummary> concepts) {
		this.concepts = concepts;
	}
	
	public ImmutableList<IndexPageItem> getItems() {
		return items;
	}
	
	public List<ContentSummary> getConcepts(){
		return concepts;
	}

}
