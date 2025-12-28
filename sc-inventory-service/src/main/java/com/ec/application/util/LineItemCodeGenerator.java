package com.ec.application.util;

import com.ec.application.model.IndentInventoryList;
import java.util.List;

public class LineItemCodeGenerator {

    /**
     * Generate initial line item code during creation
     * Format: INDENT_ID/PRODUCT_ID
     * Example: IND001/PROD-A
     */
    public static String generateInitialCode(String indentId, String productId) {
        return String.format("%s/%s", indentId, productId);
    }

    /**
     * Generate split line item codes
     * Format: PARENT_CODE/SPLIT_INDEX
     * Example: IND001/PROD-A/1, IND001/PROD-A/2
     */
    public static String generateSplitCode(String parentLineItemCode, int splitIndex) {
        return String.format("%s/%d", parentLineItemCode, splitIndex);
    }

    /**
     * Check if a line item code represents a split item
     */
    public static boolean isSplitItem(String lineItemCode) {
        if (lineItemCode == null) return false;
        return lineItemCode.chars().filter(ch -> ch == '/').count() > 1;
    }

    /**
     * Get parent code from a split item code
     */
    public static String getParentCode(String lineItemCode) {
        if (lineItemCode == null || !isSplitItem(lineItemCode)) {
            return null;
        }
        int lastSlashIndex = lineItemCode.lastIndexOf('/');
        return lineItemCode.substring(0, lastSlashIndex);
    }

    /**
     * Get the next available split index for a parent code
     */
    public static int getNextSplitIndex(String parentCode, List<IndentInventoryList> existingItems) {
        int maxIndex = 0;
        String prefix = parentCode + "/";

        for (IndentInventoryList item : existingItems) {
            String code = item.getLineItemCode();
            if (code != null && code.startsWith(prefix)) {
                try {
                    String[] parts = code.split("/");
                    int index = Integer.parseInt(parts[parts.length - 1]);
                    maxIndex = Math.max(maxIndex, index);
                } catch (NumberFormatException e) {
                    // Skip if not a number
                }
            }
        }

        return maxIndex + 1;
    }
}