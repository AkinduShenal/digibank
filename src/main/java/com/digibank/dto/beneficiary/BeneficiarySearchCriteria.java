package com.digibank.dto.beneficiary;

import com.digibank.enums.BeneficiaryStatus;
import com.digibank.enums.BeneficiaryType;
import com.digibank.util.BeneficiaryConstants;

import java.util.Locale;

public class BeneficiarySearchCriteria {

	private String query;
	private BeneficiaryType type;
	private BeneficiaryStatus status;
	private Boolean favourite;
	private Integer page;
	private Integer size;
	private String sort;
	private String direction;

	public BeneficiarySearchCriteria() {
		this.page = 0;
		this.size = BeneficiaryConstants.DEFAULT_PAGE_SIZE;
		this.sort = "beneficiaryName";
		this.direction = "asc";
	}

	public BeneficiarySearchCriteria(String query, BeneficiaryType type, BeneficiaryStatus status,
			Boolean favourite, Integer page, Integer size, String sort, String direction) {
		this.query = query;
		this.type = type;
		this.status = status;
		this.favourite = favourite;
		this.page = page;
		this.size = size;
		this.sort = sort;
		this.direction = direction;
	}

	public int safePage() {
		if (page == null || page < 0) {
			return 0;
		}
		return page;
	}

	public int safeSize() {
		if (size == null || size < 1) {
			return BeneficiaryConstants.DEFAULT_PAGE_SIZE;
		}
		return Math.min(size, BeneficiaryConstants.MAX_PAGE_SIZE);
	}

	public String safeSort() {
		String candidate = trim(sort);
		if (candidate == null || !BeneficiaryConstants.ALLOWED_SORT_FIELDS.contains(candidate)) {
			return "beneficiaryName";
		}
		return candidate;
	}

	public String safeDirection() {
		String candidate = trim(direction);
		if (candidate == null) {
			return "asc";
		}
		String normalized = candidate.toLowerCase(Locale.ROOT);
		return "desc".equals(normalized) ? "desc" : "asc";
	}

	public boolean isDeletedStatusRequested() {
		return status == BeneficiaryStatus.DELETED;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public BeneficiaryType getType() {
		return type;
	}

	public void setType(BeneficiaryType type) {
		this.type = type;
	}

	public BeneficiaryStatus getStatus() {
		return status;
	}

	public void setStatus(BeneficiaryStatus status) {
		this.status = status;
	}

	public Boolean getFavourite() {
		return favourite;
	}

	public void setFavourite(Boolean favourite) {
		this.favourite = favourite;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	private String trim(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
