package com.github.fengtan.solrgui.ui;

import java.util.ArrayList;
import java.util.List;

public class ColumnList extends ArrayList<Column> {
		
	public List<Column> getColumnsDisplayed() {
		List<Column> columns = new ArrayList<Column>();
		for (Column column: this) {
			if (column.isDisplayed()) {
				columns.add(column);
			}
		}
		return columns;
	}
	
	public boolean isColumnDisplayed(String columnTitle) {
		for (Column column: this) {
			if (column.getTitle().equals(columnTitle) && column.isDisplayed()) {
				return true;
			}
		}
		return false;
	}
	
	public void setColumnDisplayed(String columnTitle, boolean displayed) {
		for (Column column: this) {
			if (column.getTitle().equals(columnTitle)) {
				column.setDisplayed(displayed);
				return; // We assume column titles are unique.
			}
		}
	}

	public boolean contains(String title) {
		return contains(new Column(title));
	}

	public int indexOf(String title) {
		return indexOf(new Column(title));
	}
	
	public int getIndexDisplayed(String title) {
		return getColumnsDisplayed().indexOf(new Column(title));
	}

}
