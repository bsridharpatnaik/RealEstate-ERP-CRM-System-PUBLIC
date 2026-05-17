//react
import React from "react";
import Checkbox from "@material-ui/core/Checkbox";
import Table from "@material-ui/core/Table";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import TableHead from "@material-ui/core/TableHead";
import TableRow from "@material-ui/core/TableRow";
import IconButton from "@material-ui/core/IconButton";
import trashOutlineIcon from "./../../../Shared/Icons/trash-outline.png";
import ChevronLeftIcon from "@material-ui/icons/ChevronLeft";
import ChevronRightIcon from "@material-ui/icons/ChevronRight";
import TextField from "@material-ui/core/TextField";
import Tooltip from "@material-ui/core/Tooltip";
import Popper from "@material-ui/core/Popper";
import Paper from "@material-ui/core/Paper";
import Button from "@material-ui/core/Button";
import { withStyles } from "@material-ui/core/styles";
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import CircularProgress from "@material-ui/core/CircularProgress";
import { withSnackbar } from "notistack";
import { ReactComponent as SplitIcon } from "../../../Shared/Icons/square-split-horizontal.svg";
import InputAdornment from "@material-ui/core/InputAdornment";
import SearchIcon from "@material-ui/icons/Search";
import ArrowDropUpIcon from "@material-ui/icons/ArrowDropUp";
import ArrowDropDownIcon from "@material-ui/icons/ArrowDropDown";


const HtmlTooltip = withStyles((theme) => ({
  tooltip: {
    backgroundColor: "#ffffff",
    color: "rgba(0, 0, 0, 0.87)",
    maxWidth: 300,
    fontSize: theme.typography.pxToRem(12),
    border: "1px solid #e0e0e0",
    boxShadow: "0px 2px 8px rgba(0, 0, 0, 0.15)",
    padding: "12px",
  },
}))(Tooltip);

class Step1SelectIndents extends React.Component {
  state = {
    selectedCategory: 0,
    selectedRows: new Set(),
    indentData: [],
    categories: [],
    indentList: [],
    isLoading: true,
    error: null,
    splitDialogOpen: false,
    splitAnchorEl: null,
    splitRow: null,
    splitQuantity: "",
    showLeftArrow: false,
    showRightArrow: false,
    searchQuery: "",
    sortBy: null,
    sortDirection: "ASC",
  };

  categoryTabsRef = React.createRef();

  componentDidMount() {
    this.fetchIndentData();
    // Check scroll position after data loads
    setTimeout(() => {
      this.checkScrollButtons();
    }, 100);
    
    // Add resize listener to check scroll buttons on window resize
    window.addEventListener('resize', this.checkScrollButtons);
  }

  componentWillUnmount() {
    // Remove resize listener
    window.removeEventListener('resize', this.checkScrollButtons);
  }

  componentDidUpdate(prevProps, prevState) {
    // Check scroll buttons when categories change
    if (prevState.categories !== this.state.categories) {
      setTimeout(() => {
        this.checkScrollButtons();
      }, 100);
    }
  }

  checkScrollButtons = () => {
    if (!this.categoryTabsRef.current) return;
    
    const container = this.categoryTabsRef.current;
    const { scrollLeft, scrollWidth, clientWidth } = container;
    
    this.setState({
      showLeftArrow: scrollLeft > 0,
      showRightArrow: scrollLeft < scrollWidth - clientWidth - 1,
    });
  };

  scrollLeft = () => {
    if (!this.categoryTabsRef.current) return;
    const container = this.categoryTabsRef.current;
    const scrollAmount = 200; // pixels to scroll
    container.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
    setTimeout(this.checkScrollButtons, 300);
  };

  scrollRight = () => {
    if (!this.categoryTabsRef.current) return;
    const container = this.categoryTabsRef.current;
    const scrollAmount = 200; // pixels to scroll
    container.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    setTimeout(this.checkScrollButtons, 300);
  };

  transformApiData = (apiResponse) => {
    // API returns data grouped by category: { "Category 555": [...], "Category 387": [...] }
    const categories = [];
    const indentData = [];

    // Iterate through each category in the API response
    Object.keys(apiResponse).forEach((categoryName) => {
      const items = apiResponse[categoryName];

      // Add category with count
      categories.push({
        name: categoryName,
        count: items.length,
      });

      // Transform each item and add to flat indentData array
      items.forEach((item) => {
        // Format dead stock - use toalDeadStock from deadStock object with measurementUnit
        let deadStockValue = "-";
        const measurementUnit = item.measurementUnit || "No";
        if (item.deadStock && item.deadStock.toalDeadStock !== null && item.deadStock.toalDeadStock !== undefined) {
          // Handle both string and number values, format with measurementUnit
          const stockValue = item.deadStock.toalDeadStock;
          if (typeof stockValue === 'number') {
            deadStockValue = `${stockValue.toLocaleString('en-IN')} ${measurementUnit}`;
          } else if (typeof stockValue === 'string' && stockValue.trim() !== '') {
            const numValue = parseFloat(stockValue);
            if (!isNaN(numValue)) {
              deadStockValue = `${numValue.toLocaleString('en-IN')} ${measurementUnit}`;
            } else {
              deadStockValue = stockValue.trim();
            }
          }
        }

        // Use creationDate if available, otherwise fallback to indentDate
        const dateValue = item.creationDate || item.indentDate || "-";

        indentData.push({
          indentId: item.lineItemCode,
          inventoryName: item.productName || "-",
          quantity: item.quantity ? item.quantity.toString() : "0",
          unit: item.measurementUnit || "No",
          projectName: item.tenantName || "-",
          specification: item.specification || "-",
          remarks: item.remarks || "",
          deadStock: deadStockValue,
          dateCreation: dateValue,
          category: item.categoryName || categoryName,
          // Store additional fields that might be needed
          productId: item.productId,
          indentNo: item.indentNo,
          actualIndentId: item.indentId || item.indentNo, // Store actual indent ID for API calls
          lineItemStatus: item.lineItemStatus,
          lineItemCode: item.lineItemCode, // Store lineItemCode explicitly
          // Store full deadStock object for potential future use
          deadStockData: item.deadStock,
        });
      });
    });

    // Sort categories by name for consistency
    categories.sort((a, b) => a.name.localeCompare(b.name));

    return { categories, indentData };
  };

  fetchIndentData = async () => {
    this.setState({ isLoading: true, error: null });
    
    const { sortBy, sortDirection } = this.state;
    const config = {};
    if (sortBy) {
      config.params = { sortBy, direction: sortDirection };
    }

    try {
      const response = await API.GET(apiEndpoints.getOpenIndentsByCategory, Object.keys(config).length ? config : undefined);
      
      if (response.success && response.data) {
        const { categories, indentData } = this.transformApiData(response.data);
        
        // Update selected indents mapping if they exist in the new data
        if (this.props.selectedIndents && this.props.selectedIndents.length > 0) {
          const selectedRows = new Set();
          const indentList = [];

          this.props.selectedIndents.forEach((selectedIndent) => {
            const indentId = selectedIndent.indentId || selectedIndent.lineItemCode;
            // Find matching indent in the new data
            const matchingIndent = indentData.find((ind) => ind.indentId === indentId);
            if (matchingIndent) {
              selectedRows.add(indentId);
              indentList.push(matchingIndent);
            } else {
              // If not found, preserve the original selected indent
              selectedRows.add(indentId);
              indentList.push(selectedIndent);
            }
          });

          this.setState({
            categories,
            indentData,
            selectedRows,
            indentList,
            isLoading: false,
          });

          // Update parent component with the updated list
          if (indentList.length > 0) {
            this.updateIndentList(indentList);
          }
        } else {
          this.setState({
            categories,
            indentData,
            isLoading: false,
          });
        }
      } else {
        this.setState({
          error: response.errorMessage || "Failed to fetch indent data",
          isLoading: false,
        });
      }
    } catch (error) {
      this.setState({
        error: "An error occurred while fetching indent data",
        isLoading: false,
      });
    }
  };

  handleCategoryChange = (event, newValue) => {
    this.setState({ selectedCategory: newValue });
  };

  // Sortable columns: productName, indentNo, creationDate, quantity. One column at a time; click toggles ASC/DESC.
  handleSortColumn = (columnKey) => {
    const nextDirection =
      this.state.sortBy === columnKey && this.state.sortDirection === "ASC"
        ? "DESC"
        : "ASC";
    this.setState(
      { sortBy: columnKey, sortDirection: nextDirection },
      () => this.fetchIndentData()
    );
  };

  renderSortableHeader = (label, columnKey) => {
    const isActive = this.state.sortBy === columnKey;
    return (
      <TableCell
        className="sortable-header"
        onClick={() => this.handleSortColumn(columnKey)}
      >
        <span className="sortable-header-content">
          {label}
          {isActive &&
            (this.state.sortDirection === "ASC" ? (
              <ArrowDropUpIcon className="sort-indicator" />
            ) : (
              <ArrowDropDownIcon className="sort-indicator" />
            ))}
        </span>
      </TableCell>
    );
  };

  handleSelectAll = (event) => {
    const category = this.state.categories[this.state.selectedCategory].name;
    const filteredData = this.getFilteredData();
    if (event.target.checked) {
      const newSelected = new Set(this.state.selectedRows);
      filteredData.forEach((row) => {
        newSelected.add(row.indentId);
      });
      const indentList = this.state.indentData.filter((item) =>
        newSelected.has(item.indentId)
      );
      this.setState({ selectedRows: newSelected, indentList });
      this.updateIndentList(indentList);
    } else {
      const newSelected = new Set(this.state.selectedRows);
      filteredData.forEach((row) => {
        newSelected.delete(row.indentId);
      });
      const indentList = this.state.indentData.filter((item) =>
        newSelected.has(item.indentId)
      );
      this.setState({ selectedRows: newSelected, indentList });
      this.updateIndentList(indentList);
    }
  };

  handleSelectRow = (indentId) => (event) => {
    const newSelected = new Set(this.state.selectedRows);
    if (event.target.checked) {
      newSelected.add(indentId);
    } else {
      newSelected.delete(indentId);
    }
    const indentList = this.state.indentData.filter((item) =>
      newSelected.has(item.indentId)
    );
    this.setState({ selectedRows: newSelected, indentList });
    this.updateIndentList(indentList);
  };

  updateIndentList = (indentList) => {
    this.props.onSelectIndents(indentList);
    this.props.onIndentItemsChange(indentList);
  };

  handleRemoveFromList = (indentId) => () => {
    const newSelected = new Set(this.state.selectedRows);
    newSelected.delete(indentId);
    const indentList = this.state.indentData.filter((item) =>
      newSelected.has(item.indentId)
    );
    this.setState({ selectedRows: newSelected, indentList });
    this.updateIndentList(indentList);
  };



  handleSplitClick = (event, row) => {
    this.setState({
      splitDialogOpen: true,
      splitAnchorEl: event.currentTarget,
      splitRow: row,
      splitQuantity: "",
    });
  };

  handleSplitClose = () => {
    this.setState({
      splitDialogOpen: false,
      splitAnchorEl: null,
      splitRow: null,
      splitQuantity: "",
    });
  };

  handleSplitQuantityChange = (event) => {
    this.setState({ splitQuantity: event.target.value });
  };

  handleSplitSave = async () => {
    const { splitRow, splitQuantity } = this.state;
    
    if (!splitQuantity || parseFloat(splitQuantity) <= 0) {
      this.props.enqueueSnackbar("Please enter a valid quantity", {
        variant: "error",
      });
      return;
    }

    if (!splitRow) {
      this.props.enqueueSnackbar("Invalid indent selected", {
        variant: "error",
      });
      return;
    }

    const indentId = splitRow.actualIndentId || splitRow.indentNo;
    const lineItemCode = splitRow.lineItemCode || splitRow.indentId;

    if (!indentId || !lineItemCode) {
      this.props.enqueueSnackbar("Missing indent information", {
        variant: "error",
      });
      return;
    }

    try {
      const payload = {
        lineItemCode: lineItemCode,
        splitQuantity: splitQuantity,
      };

      const url = apiEndpoints.splitIndent.replace("{indentid}", indentId);
      const response = await API.PATCH(url, payload);

      if (response.success) {
        this.props.enqueueSnackbar("Indent split successfully", {
          variant: "success",
        });
        this.handleSplitClose();
        // Refresh the indent data
        this.fetchIndentData();
      } else {
        this.props.enqueueSnackbar(
          response.errorMessage || "Failed to split indent",
          { variant: "error" }
        );
      }
    } catch (error) {
      this.props.enqueueSnackbar("An error occurred while splitting indent", {
        variant: "error",
      });
    }
  };

  getFilteredData = () => {
    if (this.state.categories.length === 0 || !this.state.categories[this.state.selectedCategory]) {
      return [];
    }
    const category = this.state.categories[this.state.selectedCategory].name;
    return this.state.indentData.filter(
      (item) => item.category === category
    );
  };

  // Returns true if indent matches search (inventory name or product/line code). Empty search = no match for highlighting.
  itemMatchesSearch = (item) => {
    const q = (this.state.searchQuery || "").trim().toLowerCase();
    if (!q) return false;
    const name = (item.inventoryName || "").toLowerCase();
    const lineCode = (item.lineItemCode || item.indentId || "").toString().toLowerCase();
    const indentNo = (item.indentNo || "").toString().toLowerCase();
    return name.includes(q) || lineCode.includes(q) || indentNo.includes(q);
  };

  // Returns true if the category has at least one indent matching the current search.
  getCategoryHasSearchMatch = (categoryName) => {
    const q = (this.state.searchQuery || "").trim();
    if (!q) return false;
    return this.state.indentData.some(
      (item) => item.category === categoryName && this.itemMatchesSearch(item)
    );
  };

  handleSearchChange = (event) => {
    this.setState({ searchQuery: event.target.value });
  };

  // Helper function to group indent list items by productId
  getGroupedIndentList = () => {
    const groupedItems = {};

    this.state.indentList.forEach((item) => {
      const productId = item.productId;

      if (!groupedItems[productId]) {
        groupedItems[productId] = {
          productId: productId,
          inventoryName: item.inventoryName,
          unit: item.unit,
          projectQuantities: {},
          totalQuantity: 0,
          originalItems: [],
        };
      }

      // Add project quantity - show each indent separately even if same tenant
      const projectName = item.projectName || "-";
      const quantity = parseFloat(item.quantity || 0);

      // Instead of summing quantities by tenant, store each indent quantity separately
      if (!groupedItems[productId].projectQuantities[projectName]) {
        groupedItems[productId].projectQuantities[projectName] = [];
      }
      groupedItems[productId].projectQuantities[projectName].push(quantity);

      // Add total quantity
      groupedItems[productId].totalQuantity += quantity;

      // Store original item for removal
      groupedItems[productId].originalItems.push(item);
    });

    return Object.values(groupedItems);
  };

  // Helper function to remove all items of a product group
  handleRemoveGroupedItem = (productId) => () => {
    const newSelected = new Set(this.state.selectedRows);

    // Find all original items with this productId and remove them from selection
    this.state.indentList.forEach((item) => {
      if (item.productId === productId) {
        newSelected.delete(item.indentId);
      }
    });

    const indentList = this.state.indentData.filter((item) =>
      newSelected.has(item.indentId)
    );

    this.setState({ selectedRows: newSelected, indentList });
    this.updateIndentList(indentList);
  };

  // Helper function to format null/undefined/empty values as '-'
  formatValue = (value) => {
    if (value === null || value === undefined || value === "") {
      return "-";
    }
    return value;
  };

  // Format quantity for display to avoid floating-point artifacts (e.g. 154.67000000000002 -> 154.67)
  formatQuantityForDisplay = (num) => {
    const n = parseFloat(num);
    if (isNaN(n)) return "0";
    return Number.isInteger(n) ? String(n) : parseFloat(n.toFixed(2)).toString();
  };

  render() {
    if (this.state.isLoading) {
      return (
        <div className="step1-select-indents" style={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "400px" }}>
          <CircularProgress />
        </div>
      );
    }

    if (this.state.error) {
      return (
        <div className="step1-select-indents" style={{ padding: "20px", textAlign: "center" }}>
          <div style={{ color: "red", marginBottom: "10px" }}>{this.state.error}</div>
          <button onClick={this.fetchIndentData} style={{ padding: "8px 16px", cursor: "pointer" }}>
            Retry
          </button>
        </div>
      );
    }

    if (this.state.categories.length === 0) {
      return (
        <div className="step1-select-indents" style={{ padding: "20px", textAlign: "center" }}>
          <div>No open indents available</div>
        </div>
      );
    }

    const filteredData = this.getFilteredData();
    const isAllSelected =
      filteredData.length > 0 &&
      filteredData.every((row) => this.state.selectedRows.has(row.indentId));
    const isSomeSelected =
      filteredData.some((row) => this.state.selectedRows.has(row.indentId)) &&
      !isAllSelected;

    return (
      <div className="step1-select-indents">
        <div className="step1-main-content">
          <div className="step1-search-wrapper">
            <TextField
              value={this.state.searchQuery}
              onChange={this.handleSearchChange}
              placeholder="Search by inventory name or product code..."
              variant="outlined"
              size="small"
              fullWidth
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon fontSize="small" style={{ color: "#999" }} />
                  </InputAdornment>
                ),
                style: { fontSize: "13px", backgroundColor: "#fff" },
              }}
              className="step1-search-input"
            />
          </div>
          <div className="category-tabs-wrapper">
            {this.state.showLeftArrow && (
              <IconButton
                className="category-tabs-arrow category-tabs-arrow-left"
                onClick={this.scrollLeft}
                size="small"
              >
                <ChevronLeftIcon />
              </IconButton>
            )}
            <div
              className="category-tabs"
              ref={this.categoryTabsRef}
              onScroll={this.checkScrollButtons}
            >
              {this.state.categories.map((category, index) => (
                <div
                  key={index}
                  className={`category-tab ${this.state.selectedCategory === index ? 'active' : ''} ${this.getCategoryHasSearchMatch(category.name) ? 'search-match' : ''}`}
                  onClick={() => this.handleCategoryChange(null, index)}
                >
                  {category.name} ({category.count})
                </div>
              ))}
            </div>
            {this.state.showRightArrow && (
              <IconButton
                className="category-tabs-arrow category-tabs-arrow-right"
                onClick={this.scrollRight}
                size="small"
              >
                <ChevronRightIcon />
              </IconButton>
            )}
          </div>

          <div className="indent-table-wrapper">
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell padding="checkbox">
                    <Checkbox
                      indeterminate={isSomeSelected}
                      checked={isAllSelected}
                      onChange={this.handleSelectAll}
                    />
                  </TableCell>
                  {this.renderSortableHeader("Indent No.", "indentNo")}
                  {this.renderSortableHeader("Inventory Name", "productName")}
                  {this.renderSortableHeader("Quantity - Unit", "quantity")}
                  <TableCell>Project Name</TableCell>
                  <TableCell>Specification</TableCell>
                  <TableCell>Remarks</TableCell>
                  <TableCell>Dead Stock</TableCell>
                  {this.renderSortableHeader("Date Creation", "creationDate")}
                  <TableCell>Split Action</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredData.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={10} style={{ textAlign: "center", padding: "20px" }}>
                      No indents available in this category
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredData.map((row) => (
                    <TableRow
                      key={row.indentId}
                      hover
                      className={this.itemMatchesSearch(row) ? "search-match-row" : ""}
                    >
                      <TableCell padding="checkbox">
                        <Checkbox
                          checked={this.state.selectedRows.has(row.indentId)}
                          onChange={this.handleSelectRow(row.indentId)}
                        />
                      </TableCell>
                    <TableCell>{this.formatValue(row.indentNo || row.indentId)}</TableCell>
                    <TableCell>{this.formatValue(row.inventoryName)}</TableCell>
                    <TableCell>
                      {this.formatValue(row.quantity)} {this.formatValue(row.unit)}
                    </TableCell>
                    <TableCell>{this.formatValue(row.projectName)}</TableCell>
                    <TableCell>{this.formatValue(row.specification)}</TableCell>
                    <TableCell>
                      {row.remarks && row.remarks.trim() ? (
                        <HtmlTooltip
                          title={
                            <div style={{ textAlign: 'left', maxWidth: '300px', wordWrap: 'break-word' }}>
                              <div style={{ fontWeight: 'bold', marginBottom: '8px', fontSize: '13px', color: '#323c47' }}>
                                Remarks:
                              </div>
                              <div style={{ fontSize: '12px', color: '#666', lineHeight: '1.4' }}>
                                {row.remarks}
                              </div>
                            </div>
                          }
                          placement="top"
                          arrow
                        >
                          <div
                            style={{
                              fontSize: "12px",
                              whiteSpace: "nowrap",
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                              maxWidth: "120px",
                              cursor: "help",
                              borderBottom: "1px dotted #666",
                              padding: "4px 0"
                            }}
                          >
                            {row.remarks}
                          </div>
                        </HtmlTooltip>
                      ) : (
                        <span style={{ fontSize: "12px", color: "#999" }}>-</span>
                      )}
                    </TableCell>
                    <TableCell>
                      {row.deadStockData && 
                       row.deadStockData.detailedDeadStock && 
                       Array.isArray(row.deadStockData.detailedDeadStock) && 
                       row.deadStockData.detailedDeadStock.length > 0 &&
                       row.deadStock !== "-" ? (
                        <HtmlTooltip
                          title={
                            <div style={{ textAlign: 'left' }}>
                              <div style={{ fontWeight: 'bold', marginBottom: '8px', fontSize: '13px', color: '#323c47' }}>
                                Dead Stock Breakdown:
                              </div>
                              {row.deadStockData.detailedDeadStock.map((item, idx) => {
                                const tenantName = Object.keys(item)[0];
                                const amount = item[tenantName];
                                if (amount === null || amount === undefined) return null;
                                const unit = row.unit || "No";
                                const formattedAmount = typeof amount === 'number' 
                                  ? `${amount.toLocaleString('en-IN')} ${unit}`
                                  : `${amount} ${unit}`;
                                return (
                                  <div key={idx} style={{ marginBottom: '4px', fontSize: '12px', color: '#666' }}>
                                    <span style={{ fontWeight: '500', color: '#323c47' }}>{tenantName}:</span> {formattedAmount}
                                  </div>
                                );
                              })}
                            </div>
                          }
                          placement="top"
                          arrow
                        >
                          <span style={{ cursor: 'help', borderBottom: '1px dotted #666' }}>
                            {this.formatValue(row.deadStock)}
                          </span>
                        </HtmlTooltip>
                      ) : (
                        <span>{this.formatValue(row.deadStock)}</span>
                      )}
                    </TableCell>
                    <TableCell>{this.formatValue(row.dateCreation)}</TableCell>
                      <TableCell align="center">
                      <IconButton
                        size="small"
                        onClick={(e) => this.handleSplitClick(e, row)}
                        ref={(el) => {
                          if (el && this.state.splitRow?.indentId === row.indentId) {
                            // Store ref if needed
                          }
                        }}
                      >
                        <SplitIcon
                          width={18}
                          height={18}
                          style={{ display: "block" }}
                        />
                      </IconButton>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
            <div className="table-footer">
              <div className="selected-count">
                {this.state.selectedRows.size} of {this.state.indentData.length}{" "}
                row(s) selected.
              </div>
            </div>
          </div>
        </div>
        {this.state.indentList.length > 0 && (
          <div className="step1-side-panel">
            <div className="indent-list-header">
              Indent List ({this.state.indentList.length})
            </div>
            <div className="indent-list-items">
              {this.getGroupedIndentList().map((groupedItem) => (
                <div key={groupedItem.productId} className="indent-list-item">
                  <div className="item-info">
                    <div className="item-name">{this.formatValue(groupedItem.inventoryName)}</div>
                    <div className="item-quantity">
                      {groupedItem.originalItems.length > 1 ? (
                        <HtmlTooltip
                          title={
                            <div style={{ textAlign: 'left' }}>
                              <div style={{ fontWeight: 'bold', marginBottom: '8px', fontSize: '13px', color: '#323c47' }}>
                                Quantity by Project:
                              </div>
                              {Object.entries(groupedItem.projectQuantities).map(([project, quantities], idx) => (
                                quantities.map((qty, qtyIdx) => (
                                  <div key={`${idx}-${qtyIdx}`} style={{ marginBottom: '4px', fontSize: '12px', color: '#666' }}>
                                    <span style={{ fontWeight: '500', color: '#323c47' }}>{project}:</span> {this.formatQuantityForDisplay(qty)} {this.formatValue(groupedItem.unit)}
                                  </div>
                                ))
                              ))}
                              <div style={{ marginTop: '8px', fontSize: '12px', fontWeight: 'bold', color: '#323c47', borderTop: '1px solid #e0e0e0', paddingTop: '4px' }}>
                                Total: {this.formatQuantityForDisplay(groupedItem.totalQuantity)} {this.formatValue(groupedItem.unit)}
                              </div>
                            </div>
                          }
                          placement="left"
                          arrow
                        >
                          <span style={{ cursor: 'help', borderBottom: '1px dotted #666' }}>
                            {this.formatQuantityForDisplay(groupedItem.totalQuantity)} - {this.formatValue(groupedItem.unit)}
                          </span>
                        </HtmlTooltip>
                      ) : (
                        <span>{this.formatQuantityForDisplay(groupedItem.totalQuantity)} - {this.formatValue(groupedItem.unit)}</span>
                      )}
                    </div>
                  </div>
                  <IconButton
                    size="small"
                    onClick={this.handleRemoveGroupedItem(groupedItem.productId)}
                    className="delete-button"
                    disableRipple
                  >
                    <img src={trashOutlineIcon} alt="Delete" style={{ width: 15, height: 15 }} />
                  </IconButton>
                </div>
              ))}
            </div>
          </div>
        )}
        <Popper
          open={this.state.splitDialogOpen}
          anchorEl={this.state.splitAnchorEl}
          placement="bottom"
          style={{ zIndex: 1300 }}
        >
          <Paper className="split-dialog" style={{ padding: "16px", minWidth: "250px", boxShadow: "0px 4px 12px rgba(0, 0, 0, 0.15)" }}>
            <div style={{ marginBottom: "12px", textAlign: "left", fontWeight: "500" }}>
              Split Action
            </div>
            <TextField
              value={this.state.splitQuantity}
              onChange={this.handleSplitQuantityChange}
              placeholder={"Enter quantity"
              }
              variant="outlined"
              fullWidth
              size="small"
              type="number"
              inputProps={{ style: { fontSize: "12px", padding: "8px" } }}
              style={{ marginBottom: "16px" }}
            />
            <div className="split-dialog-actions">
              <Button onClick={this.handleSplitClose} color="default" size="small">
                Cancel
              </Button>
              <Button onClick={this.handleSplitSave} color="primary" variant="contained" size="small">
                Save
              </Button>
            </div>
          </Paper>
        </Popper>
      </div>
    );
  }
}

export default withSnackbar(Step1SelectIndents);
