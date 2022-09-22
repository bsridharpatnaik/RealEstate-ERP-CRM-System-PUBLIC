package com.ec.application.data;


public class BOQStatusDetailsMapKey {

	private String usageLocationId;
	private String productId;
	
//	public long getUsageLocationId() {
//		return usageLocationId;
//	}
//	public void setUsageLocationId(long usageLocationId) {
//		this.usageLocationId = usageLocationId;
//	}
	
//	public long getProductId() {
//		return productId;
//	}
//	public void setProductId(long productId) {
//		this.productId = productId;
//	}
//	
//	public BOQStatusDetailsMapKey(long usageLocationId, long productId) {
//		this.usageLocationId = usageLocationId;
//		this.productId = productId;
//	}
	@Override
	public String toString() {
		return "[usageLocationId=" + usageLocationId + ", productId=" + productId + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((productId == null) ? 0 : productId.hashCode());
		result = prime * result + ((usageLocationId == null) ? 0 : usageLocationId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BOQStatusDetailsMapKey other = (BOQStatusDetailsMapKey) obj;
		if (productId == null) {
			if (other.productId != null)
				return false;
		} else if (!productId.equals(other.productId))
			return false;
		if (usageLocationId == null) {
			if (other.usageLocationId != null)
				return false;
		} else if (!usageLocationId.equals(other.usageLocationId))
			return false;
		return true;
	}

	public String getUsageLocationId() {
		return usageLocationId;
	}
	public void setUsageLocationId(String usageLocationId) {
		this.usageLocationId = usageLocationId;
	}
	public String getProductId() {
		return productId;
	}
	public void setProductId(String productId) {
		this.productId = productId;
	}
	public BOQStatusDetailsMapKey() {
		super();
	}
	
	
}
