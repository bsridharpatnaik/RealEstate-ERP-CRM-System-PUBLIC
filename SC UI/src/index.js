import React from "react";
import ReactDOM from "react-dom";
import "./index.css";
import App from "./App";
import * as serviceWorker from "./serviceWorker";
import { SnackbarProvider } from "notistack";
import { Provider } from "react-redux";
import rootReducer from "./reducers/index.js";

import { createStore, applyMiddleware } from "redux";
import thunk from "redux-thunk";

export const store = createStore(rootReducer, applyMiddleware(thunk));
ReactDOM.render(
  <React.StrictMode>
    <Provider store={store}>
      <SnackbarProvider
        maxSnack={3}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      >
        <App />
      </SnackbarProvider>
    </Provider>
  </React.StrictMode>,
  document.getElementById("root")
);

// Prevent scroll wheel from changing number input values
document.addEventListener('wheel', () => {
  if (document.activeElement && document.activeElement.type === 'number') {
    document.activeElement.blur();
  }
}, { passive: true });

// Translate vertical mouse-wheel into horizontal scroll over any wide table so
// external-mouse users can reach off-screen columns/actions without a
// horizontal wheel. Delegated once here to cover every `.table-wrapper` on all
// pages (shared and hand-written).
document.addEventListener('wheel', (e) => {
  if (e.deltaY === 0 || e.ctrlKey) return; // ctrl+wheel = browser zoom
  const el = e.target.closest && e.target.closest('.table-wrapper, .x-scroll');
  if (!el || el.scrollWidth <= el.clientWidth) return; // no horizontal overflow
  const atStart = el.scrollLeft <= 0 && e.deltaY < 0;
  const atEnd =
    el.scrollLeft + el.clientWidth >= el.scrollWidth - 1 && e.deltaY > 0;
  if (atStart || atEnd) return; // let the page scroll vertically at the edges
  el.scrollLeft += e.deltaY;
  e.preventDefault();
}, { passive: false });

// Pin the action column (View/Edit/Delete buttons) to the right edge of any
// wide table so it stays reachable while scrolling. Generic: the action cells
// are detected as the trailing run of cells whose class contains "action"
// (works for the single-cell Indent layout and the multi-cell PO/Inward ones
// alike) — no assumption about the first column or a fixed column count. Only
// active when the table actually overflows, so tables that fit are untouched.
(function setupStickyActionColumn() {
  const isAction = (cell) =>
    cell && cell.className && /(^|[\s-])action/i.test(cell.className);

  // Trailing run of action cells in a row, right-to-left.
  const trailingActionCells = (rowEl) => {
    const cells = Array.from(rowEl.children);
    const out = [];
    for (let i = cells.length - 1; i >= 0 && isAction(cells[i]); i--) {
      out.push(cells[i]);
    }
    return out; // [rightmost, ..., leftmost]
  };

  const clearCell = (c) => {
    c.style.position = '';
    c.style.right = '';
    c.style.background = '';
    c.style.zIndex = '';
    c.style.borderBottom = '';
    c.classList.remove('pinned-action-edge');
  };

  const pinTable = (wrapper) => {
    const table = wrapper.querySelector(':scope > table');
    if (!table) return;
    const rows = table.querySelectorAll(
      ':scope > thead > tr, :scope > tbody > tr'
    );
    const overflow = table.scrollWidth > wrapper.clientWidth + 1;
    rows.forEach((row) => {
      const pinned = trailingActionCells(row); // right-to-left
      // The opaque cell background (needed to hide scrolled content) would
      // paint over the row's separator, which is drawn as an inset box-shadow
      // on the <tr>. Re-draw it as a real border on the pinned cells — skip the
      // last row, which has no separator.
      const noSeparator = row.classList.contains('last');
      let right = 0;
      pinned.forEach((cell, idx) => {
        clearCell(cell);
        if (!overflow) return;
        cell.style.position = 'sticky';
        cell.style.right = right + 'px';
        cell.style.background = '#fff';
        cell.style.zIndex = row.parentElement.tagName === 'THEAD' ? '3' : '2';
        if (!noSeparator) cell.style.borderBottom = '1px solid #a2a2c3';
        if (idx === pinned.length - 1) cell.classList.add('pinned-action-edge');
        right += cell.getBoundingClientRect().width;
      });
    });
    updateShadow(wrapper);
    if (!wrapper.dataset.pinScroll) {
      wrapper.dataset.pinScroll = '1';
      wrapper.addEventListener('scroll', () => updateShadow(wrapper), {
        passive: true,
      });
    }
  };

  const updateShadow = (wrapper) => {
    const hiddenRight =
      wrapper.scrollLeft + wrapper.clientWidth < wrapper.scrollWidth - 1;
    wrapper.classList.toggle('pin-shadow', hiddenRight);
  };

  const pinAll = () =>
    document.querySelectorAll('.table-wrapper').forEach(pinTable);

  let timer = null;
  const schedule = () => {
    if (timer) return;
    // setTimeout (not requestAnimationFrame) so it still fires when the tab is
    // backgrounded — rAF is paused for hidden tabs.
    timer = setTimeout(() => {
      timer = null;
      observer.disconnect();
      pinAll();
      observe();
    }, 60);
  };

  const observer = new MutationObserver(schedule);
  const observe = () =>
    observer.observe(document.body, { childList: true, subtree: true });

  window.addEventListener('resize', schedule, { passive: true });
  observe();
  schedule();
})();

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.register();
