/**
 * Deterministic per-project color palette.
 * Each project code always maps to the same color across all report pages.
 */


const PROJECT_PALETTE = [
  { bg: '#e8f4fd', color: '#1a6fa8', border: '#aed6f1' }, // blue
  { bg: '#eafaf1', color: '#1e8449', border: '#a9dfbf' }, // green
  { bg: '#fef9e7', color: '#9a7d0a', border: '#f9e79f' }, // yellow
  { bg: '#fdf2e9', color: '#ca6f1e', border: '#f0b27a' }, // orange
  { bg: '#f5eef8', color: '#7d3c98', border: '#d7bde2' }, // purple
  { bg: '#fdedec', color: '#cb4335', border: '#f1948a' }, // red
  { bg: '#e8f6f3', color: '#117a65', border: '#76d7c4' }, // teal
  { bg: '#fef5e7', color: '#a04000', border: '#f8c471' }, // amber
  { bg: '#eaf2ff', color: '#1f618d', border: '#85c1e9' }, // navy
  { bg: '#f0f3ff', color: '#4a4aaa', border: '#b3b3e0' }, // indigo
  { bg: '#fff0f6', color: '#ad1457', border: '#f48fb1' }, // pink
  { bg: '#f0fff4', color: '#2e7d32', border: '#81c784' }, // forest green
];

/**
 * Returns a stable { bg, color, border } style object for the given project code.
 * Same code always returns the same color.
 */
export function getProjectColor(projectCode) {
  if (!projectCode) return PROJECT_PALETTE[0];
  let hash = 0;
  for (let i = 0; i < projectCode.length; i++) {
    hash = (hash * 31 + projectCode.charCodeAt(i)) & 0x7fffffff;
  }
  return PROJECT_PALETTE[hash % PROJECT_PALETTE.length];
}
