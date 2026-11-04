const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function load(fileName) {
  const filePath = path.join(__dirname, '..', fileName);
  const context = vm.createContext({ console });
  new vm.Script(fs.readFileSync(filePath, 'utf8'), { filename: filePath }).runInContext(context);
  return (expression) => vm.runInContext(expression, context);
}

const emailAlerts = load('Canh bao cong no.gs');
emailAlerts('weights = {"Đợt 1+2":1,"Đợt 3+4":2,"Đợt 5+6":3,"Đợt 7":4}');
assert.equal(emailAlerts('isDebtAlertReportLater_({dot:"Đợt 1+2",thang:"1"},{dot:"Đợt 7",thang:"12"},weights)'), true);
assert.equal(emailAlerts('isDebtAlertReportLater_({dot:"Đợt 3+4",thang:"10"},{dot:"Đợt 1+2",thang:"10"},weights)'), true);

const crossCheck = load('Ket qua kiem tra cheo.gs');
assert.equal(crossCheck('compareCrossCheckRecency_({dot:"Đợt 1+2",thang:"1"},{dot:"Đợt 7",thang:"12"})'), 1);
assert.equal(crossCheck('compareCrossCheckRecency_({dot:"Đợt 5+6",thang:"10"},{dot:"Đợt 3+4",thang:"10"}) > 0'), true);

const webApp = load('PVT_WebApp.gs');
webApp('weights = {"Đợt 1+2":1,"Đợt 3+4":2,"Đợt 5+6":3,"Đợt 7":4}');
assert.equal(webApp('pvtCompareReportRecency_({batch:"Đợt 1+2",month:"1"},{batch:"Đợt 7",month:"12"},weights)'), 1);
assert.equal(webApp('pvtFindNewestReport_([{batch:"Đợt 7",month:"12"},{batch:"Đợt 1+2",month:"1"}]).month'), '1');

console.log('report-recency tests passed');
