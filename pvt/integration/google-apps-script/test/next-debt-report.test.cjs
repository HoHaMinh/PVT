const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const sourcePath = path.join(__dirname, '..', 'Tu dong tao file dot moi.gs');
const source = fs.readFileSync(sourcePath, 'utf8');
const context = vm.createContext({ console });
new vm.Script(source, { filename: sourcePath }).runInContext(context);

function evaluate(expression) {
  return vm.runInContext(expression, context);
}

assert.deepEqual(
  { ...evaluate('calculateNextDebtReportPeriod_("Đợt 1+2", 10, false)') },
  { periodCode: '3+4', month: 10 }
);
assert.deepEqual(
  { ...evaluate('calculateNextDebtReportPeriod_("3+4", "tháng 10", false)') },
  { periodCode: '5+6', month: 10 }
);
assert.deepEqual(
  { ...evaluate('calculateNextDebtReportPeriod_("5+6", 10, true)') },
  { periodCode: '7', month: 10 }
);
assert.deepEqual(
  { ...evaluate('calculateNextDebtReportPeriod_("5+6", 10, false)') },
  { periodCode: '1+2', month: 11 }
);
assert.deepEqual(
  { ...evaluate('calculateNextDebtReportPeriod_("Đợt 7", 12, false)') },
  { periodCode: '1+2', month: 1 }
);

assert.equal(evaluate('normalizeDebtReportPeriod_(" Đợt 5+6 ")'), '5+6');
assert.equal(evaluate('parseDebtReportMonth_("Tháng 12")'), 12);
assert.equal(evaluate('parseDebtReportMonth_("13")'), 0);

context.rolloverReports = [
  { periodCode: '7', month: 12, sheet: { getName: () => '12' } },
  { periodCode: '1+2', month: 1, sheet: { getName: () => '1' } }
];
assert.equal(evaluate('findNewestDebtReport_(rolloverReports).month'), 1);
context.singleReport = [
  { periodCode: '5+6', month: 10, sheet: { getName: () => '4' } }
];
assert.equal(evaluate('findNewestDebtReport_(singleReport).sheet.getName()'), '4');
context.normalConsecutiveMonths = [
  { periodCode: '7', month: 11, sheet: { getName: () => '11' } },
  { periodCode: '1+2', month: 12, sheet: { getName: () => '12' } }
];
assert.equal(evaluate('findNewestDebtReport_(normalConsecutiveMonths).month'), 12);

const sectionColumn = [
  ['BIÊN BẢN BÁO CÁO HÓA ĐƠN CÔNG NỢ'],
  [''],
  [''],
  ['I. Hóa đơn phát sinh'],
  [''],
  ['II. Hóa đơn tồn cũ'],
  [''],
  [''],
  ['III. Hóa đơn sắp đến hạn'],
  [''],
  ['IV. Hồ sơ đã thanh toán'],
  [''],
  ['V. Hồ sơ chuyển kỳ'],
  ['']
];
context.sectionSheet = {
  getLastRow: () => sectionColumn.length,
  getName: () => '4',
  getRange: (row, column, rowCount, columnCount) => ({
    getDisplayValues: () => sectionColumn.slice(row - 1, row - 1 + rowCount)
  })
};
const sections = evaluate('getDebtReportSections_(sectionSheet)');
assert.equal(sections.I.startRow, 5);
assert.equal(sections.I.endRow, 5);
assert.equal(sections.II.startRow, 7);
assert.equal(sections.II.endRow, 8);
assert.equal(sections.III.startRow, 10);
assert.equal(sections.III.endRow, 10);

function row(...values) {
  const result = Array(17).fill('');
  values.forEach(([column, value]) => { result[column - 1] = value; });
  return result;
}

class MockRange {
  constructor(sheet, startRow, startColumn, rowCount, columnCount) {
    this.sheet = sheet;
    this.startRow = startRow;
    this.startColumn = startColumn;
    this.rowCount = rowCount;
    this.columnCount = columnCount;
  }
  getDisplayValues() {
    return this.sheet.rows.slice(this.startRow - 1, this.startRow - 1 + this.rowCount)
      .map((sourceRow) => sourceRow.slice(this.startColumn - 1,
        this.startColumn - 1 + this.columnCount).map(String));
  }
  copyTo(destination) {
    const copied = this.sheet.rows.slice(this.startRow - 1, this.startRow - 1 + this.rowCount)
      .map((sourceRow) => sourceRow.slice(this.startColumn - 1,
        this.startColumn - 1 + this.columnCount));
    copied.forEach((sourceRow, rowOffset) => {
      sourceRow.forEach((value, columnOffset) => {
        destination.sheet.rows[destination.startRow - 1 + rowOffset]
          [destination.startColumn - 1 + columnOffset] = value;
      });
    });
    return destination;
  }
  clearContent() {
    for (let r = 0; r < this.rowCount; r++) {
      for (let c = 0; c < this.columnCount; c++) {
        this.sheet.rows[this.startRow - 1 + r][this.startColumn - 1 + c] = '';
      }
    }
    return this;
  }
}

class MockSheet {
  constructor(rows) { this.rows = rows; }
  getLastRow() { return this.rows.length; }
  getLastColumn() { return 17; }
  getName() { return '4'; }
  getRange(rowNumber, columnNumber, rowCount = 1, columnCount = 1) {
    return new MockRange(this, rowNumber, columnNumber, rowCount, columnCount);
  }
  insertRowsBefore(rowNumber, count) {
    this.rows.splice(rowNumber - 1, 0, ...Array.from({ length: count }, () => row()));
  }
  setRowHeight() {}
  getRowHeight() { return 21; }
}

context.transformSheet = new MockSheet([
  row([1, 'BIÊN BẢN BÁO CÁO HÓA ĐƠN CÔNG NỢ']),
  row([4, 'Đợt 1+2'], [6, 11]),
  row([1, 'TT'], [2, 'NCC']),
  row([1, 'I. Phát sinh']),
  row([1, 1], [2, 'I-NCC'], [3, 'I-HD']),
  row(),
  row([1, 'II. Tồn cũ']),
  row([1, 1], [2, 'II-NCC'], [3, 'II-HD']),
  row(),
  row([1, 'III. Đến hạn']),
  row([1, 1], [2, 'III-A'], [3, 'HD-A']),
  row([1, 2], [2, 'III-B'], [3, 'HD-B']),
  row([1, 'IV. Đã xong']),
  row([1, 1], [2, 'IV-NCC'], [3, 'IV-HD']),
  row([1, 'V. Chuyển kỳ']),
  row([1, 1], [2, 'V-NCC'], [3, 'V-HD']),
  row([1, 'VI. Lưu']),
  row([1, 1], [2, 'VI-NCC'], [3, 'VI-HD'])
]);
evaluate('prepareNextDebtReportSheet_(transformSheet)');
const transformedSections = evaluate('getDebtReportSections_(transformSheet)');
assert.equal(context.transformSheet.rows[transformedSections.II.startRow - 1][1], 'II-NCC');
assert.equal(context.transformSheet.rows[transformedSections.II.startRow][1], 'III-A');
assert.equal(context.transformSheet.rows[transformedSections.II.startRow + 1][1], 'III-B');
for (const code of ['I', 'III', 'IV', 'V']) {
  const section = transformedSections[code];
  for (let r = section.startRow; r <= section.endRow; r++) {
    assert.equal(context.transformSheet.rows[r - 1].every((value) => value === ''), true);
  }
}
assert.equal(context.transformSheet.rows[transformedSections.VI.startRow - 1][1], 'VI-NCC');

console.log('next-debt-report tests passed');
