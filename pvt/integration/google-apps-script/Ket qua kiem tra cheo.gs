/**
 * Hàm chính thực hiện kiểm tra chéo toàn bộ file nhân viên từ file trung tâm.
 * Giữ nguyên contract hiện tại: sheet "Danh sach nhan vien" dùng cột A:B để đọc tên và link file.
 */
function runCrossCheckValidationTest() {
  const CONFIG_SHEET_NAME = "Danh sach nhan vien";

  const ssMaster = SpreadsheetApp.getActiveSpreadsheet();
  const configSheet = ssMaster.getSheetByName(CONFIG_SHEET_NAME);
  const reportSheet = getOrCreateCrossCheckReportSheet_(ssMaster);

  if (!configSheet) {
    Logger.log("Không tìm thấy sheet cấu hình nhân viên.");
    return;
  }

  clearCrossCheckReportRows_(reportSheet);

  const lastRowConfig = configSheet.getLastRow();
  if (lastRowConfig < 4) return;

  const configData = configSheet.getRange(4, 1, lastRowConfig - 3, 2).getValues();
  let testResults = [];

  // Duyệt qua từng người phụ trách từ cấu hình trung tâm.
  for (let k = 0; k < configData.length; k++) {
    let employeeName = String(configData[k][0]).trim();
    let rawSpreadsheetId = String(configData[k][1]).trim();

    if (!employeeName || !rawSpreadsheetId) continue;

    try {
      let spreadsheetId = extractSpreadsheetIdFromValue_(rawSpreadsheetId);
      let empSpreadsheet = SpreadsheetApp.openById(spreadsheetId);
      testResults.push(validateSpreadsheetCrossCheck_(empSpreadsheet, employeeName));
    } catch (e) {
      testResults.push([employeeName, "LỖI HỆ THỐNG", "Không thể xử lý file: " + e.toString()]);
    }
  }

  writeCrossCheckReportRows_(reportSheet, testResults);
}

/**
 * Chạy kiểm tra chéo ngay trong file nhân viên đang mở.
 * Dùng cho menu trong từng file user/template, không cần sheet "Danh sach nhan vien".
 */
function runCrossCheckCurrentFile() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const reportSheet = getOrCreateCrossCheckReportSheet_(ss);
  const result = validateSpreadsheetCrossCheck_(ss, ss.getName());

  clearCrossCheckReportRows_(reportSheet);
  writeCrossCheckReportRows_(reportSheet, [result]);
  ss.toast("Đã chạy kiểm tra chéo cho file hiện tại.", "Công nợ", 5);
}

function validateSpreadsheetCrossCheck_(empSpreadsheet, employeeName) {
  let validSheets = findValidCrossCheckSheets_(empSpreadsheet);

  if (validSheets.length < 2) {
    return createCrossCheckResultRow_(employeeName, "LỖI SỐ LƯỢNG", "Đang không đủ BÁO CÁO HÓA ĐƠN CÔNG NỢ để kiểm tra chéo", getNewestCrossCheckInfo_(validSheets));
  }
  if (validSheets.length > 2) {
    return createCrossCheckResultRow_(employeeName, "LỖI SỐ LƯỢNG", "Đang có nhiều hơn 2 BÁO CÁO HÓA ĐƠN CÔNG NỢ để kiểm tra chéo", getNewestCrossCheckInfo_(validSheets));
  }

  // Quy tắc 4: Kiểm tra trùng đợt + tháng
  let sheetA = validSheets[0];
  let sheetB = validSheets[1];
  if (sheetA.dot === sheetB.dot && sheetA.thang === sheetB.thang) {
    return createCrossCheckResultRow_(employeeName, "LỖI TRÙNG ĐỢT", `2 sheet BÁO CÁO HÓA ĐƠN CÔNG NỢ đang khai báo trùng đợt (${sheetA.dot} - ${sheetA.thang})`, getNewestCrossCheckInfo_(validSheets));
  }

  // Quy tắc 5: Xác định sheet cũ và mới dựa trên trọng số thời gian.
  let itemOld;
  let itemNew;
  let sheetOld;
  let sheetNew;
  if (compareCrossCheckRecency_(sheetA, sheetB) > 0) {
    itemNew = sheetA;
    itemOld = sheetB;
  } else {
    itemNew = sheetB;
    itemOld = sheetA;
  }
  sheetNew = itemNew.sheetObj;
  sheetOld = itemOld.sheetObj;
  const latestInfo = createCrossCheckLatestInfo_(itemNew);

  // Biểu mẫu hiện tại dùng đến cột Q. Đọc đủ 17 cột để kiểm tra thêm:
  // K: lũy kế đầu kỳ, M: số tiền còn lại, O: Nội địa/NK, P/Q: ngày NK.
  let dataOld = sheetOld.getRange(1, 1, sheetOld.getLastRow(), 17).getValues();
  let dataNew = sheetNew.getRange(1, 1, sheetNew.getLastRow(), 17).getValues();

  // Quy tắc dữ liệu mới được kiểm tra độc lập trên cả hai sheet trước khi
  // chạy hai phép đối chiếu hóa đơn hiện hữu.
  let rowValidation = validateCrossCheckBusinessRows_([
    { label: `sheet cũ ${sheetOld.getName()}`, data: dataOld },
    { label: `sheet mới ${sheetNew.getName()}`, data: dataNew }
  ]);
  if (!rowValidation.success) {
    return createCrossCheckResultRow_(employeeName, "THẤT BẠI DỮ LIỆU", rowValidation.message, latestInfo);
  }

  let openingBalanceCheck = compareOpeningCumulativeAmounts_(dataOld, dataNew);
  if (!openingBalanceCheck.success) {
    return createCrossCheckResultRow_(employeeName, "THẤT BẠI LŨY KẾ", openingBalanceCheck.message, latestInfo);
  }

  // Trích xuất danh sách hóa đơn theo các phần cụ thể bằng các cột định danh cốt lõi.
  let old_II_III = extractInvoicesBySections(dataOld, ["II.", "III."]);
  let new_I_II = extractInvoicesBySections(dataNew, ["I.", "II."]);

  let old_VI = extractInvoicesBySections(dataOld, ["VI."]);
  let new_I = extractInvoicesBySections(dataNew, ["I."]);
  let new_IV = extractInvoicesBySections(dataNew, ["IV."]);
  let new_V_VI = extractInvoicesBySections(dataNew, ["V.", "VI."]);

  let leftRule7 = [].concat(old_VI, new_I, new_IV);

  // Quy tắc 6: Đối chiếu Phần II + III cũ so với Phần I + II mới.
  let checkRule6 = compareInvoiceLists(old_II_III, new_I_II);
  if (!checkRule6.success) {
    return createCrossCheckResultRow_(employeeName, "THẤT BẠI QT6", `Lỗi phần II+III cũ vs I+II mới: ${checkRule6.message}`, latestInfo);
  }

  // Quy tắc 7: Đối chiếu (VI cũ + I mới + IV mới) so với (V + VI mới).
  let checkRule7 = compareInvoiceLists(leftRule7, new_V_VI);
  if (!checkRule7.success) {
    return createCrossCheckResultRow_(employeeName, "THẤT BẠI QT7", `Lỗi phần (VI cũ + I,IV mới) vs (V+VI mới): ${checkRule7.message}`, latestInfo);
  }

  return createCrossCheckResultRow_(employeeName, "PASSED", `Khớp 100% dữ liệu hóa đơn giữa sheet cũ (${sheetOld.getName()}) và sheet mới (${sheetNew.getName()})`, latestInfo);
}

/**
 * Kiểm tra các quy định nội bộ trên từng dòng hồ sơ:
 * - M > 0 không được nằm trong mục I, IV, V hoặc VI.
 * - Cột O (Nội địa hay NK) bắt buộc có giá trị.
 * - M phải bằng E - K - L; K/L trống được tính là 0.
 * - Hồ sơ NK phải có ngày vận đơn và ngày tờ khai hợp lệ.
 *
 * Chỉ kiểm tra các dòng nằm sau một tiêu đề phần La Mã (I., II., ...), giống
 * phạm vi quét của logic kiểm tra chéo cũ. Trong phạm vi đó, một dòng được xem
 * là hồ sơ khi có NCC hoặc số hóa đơn. Nhờ vậy các dòng cấu hình đầu biểu mẫu
 * như "ĐỢT BÁO CÁO" không bị nhận nhầm thành hóa đơn.
 */
function validateCrossCheckBusinessRows_(sheetInputs) {
  const forbiddenSections = { "I": true, "IV": true, "V": true, "VI": true };
  const errors = [];

  sheetInputs.forEach(function(input) {
    let currentSection = "";
    for (let r = 0; r < input.data.length; r++) {
      const row = input.data[r];
      const section = getCrossCheckSectionCode_(row[0]);
      if (section) {
        currentSection = section;
        continue;
      }
      if (!currentSection) continue;
      if (!isCrossCheckInvoiceRow_(row)) continue;

      const rowNumber = r + 1;
      const invoiceLabel = crossCheckInvoiceLabel_(row);
      const invoiceAmount = crossCheckNumber_(row[4]); // E
      const openingCumulativeAmount = crossCheckNumber_(row[10]); // K
      const paidThisPeriodAmount = crossCheckNumber_(row[11]); // L
      const remainingAmount = crossCheckNumber_(row[12]); // M
      const domesticOrImport = String(row[14] === null || row[14] === undefined ? "" : row[14]).trim(); // O
      const normalizedDomesticOrImport = domesticOrImport.toUpperCase().replace(/\s/g, "");
      const billOfLadingDate = row[15]; // P
      const customsDeclarationDate = row[16]; // Q

      const invalidNumberColumns = [];
      if (!isCrossCheckNumericValue_(row[4], false)) invalidNumberColumns.push("E - Trị giá VND/USD");
      if (!isCrossCheckNumericValue_(row[10], true)) invalidNumberColumns.push("K - Lũy kế đầu kỳ");
      if (!isCrossCheckNumericValue_(row[11], true)) invalidNumberColumns.push("L - Thanh toán đợt này");
      if (!isCrossCheckNumericValue_(row[12], false)) invalidNumberColumns.push("M - Số tiền còn lại");
      if (invalidNumberColumns.length > 0) {
        errors.push(`${input.label}, dòng ${rowNumber} (${invoiceLabel}): ${invalidNumberColumns.join(", ")} phải là số; K/L được phép để trống và được tính là 0`);
      } else {
        const expectedRemainingAmount = invoiceAmount - openingCumulativeAmount - paidThisPeriodAmount;
        if (Math.abs(remainingAmount - expectedRemainingAmount) > 0.01) {
          errors.push(`${input.label}, dòng ${rowNumber} (${invoiceLabel}): cột M ${remainingAmount.toLocaleString("vi-VN")} không khớp E - K - L = ${expectedRemainingAmount.toLocaleString("vi-VN")} (${invoiceAmount.toLocaleString("vi-VN")} - ${openingCumulativeAmount.toLocaleString("vi-VN")} - ${paidThisPeriodAmount.toLocaleString("vi-VN")})`);
        }
      }

      if (remainingAmount > 0 && forbiddenSections[currentSection]) {
        errors.push(`${input.label}, dòng ${rowNumber} (${invoiceLabel}): số tiền còn lại ${remainingAmount.toLocaleString("vi-VN")} > 0 nhưng đang nằm ở mục ${currentSection}`);
      }
      if (!domesticOrImport) {
        errors.push(`${input.label}, dòng ${rowNumber} (${invoiceLabel}): cột Nội địa hay NK đang để trống`);
      }
      if (normalizedDomesticOrImport === "NK") {
        const missingImportDates = [];
        if (isCrossCheckBlank_(billOfLadingDate)) missingImportDates.push("Ngày vận đơn");
        if (isCrossCheckBlank_(customsDeclarationDate)) missingImportDates.push("Ngày tờ khai");
        if (missingImportDates.length > 0) {
          errors.push(`${input.label}, dòng ${rowNumber} (${invoiceLabel}): đã chọn NK nhưng đang thiếu ${missingImportDates.join(" và ")}`);
        }
        const invalidImportDates = [];
        if (!isCrossCheckBlank_(billOfLadingDate) && !isCrossCheckValidDate_(billOfLadingDate)) {
          invalidImportDates.push("Ngày vận đơn");
        }
        if (!isCrossCheckBlank_(customsDeclarationDate) && !isCrossCheckValidDate_(customsDeclarationDate)) {
          invalidImportDates.push("Ngày tờ khai");
        }
        if (invalidImportDates.length > 0) {
          errors.push(`${input.label}, dòng ${rowNumber} (${invoiceLabel}): ${invalidImportDates.join(" và ")} không phải ngày hợp lệ (cần là ô ngày Google Sheets hoặc dd/MM/yyyy)`);
        }
      }
    }
  });

  return errors.length === 0
    ? { success: true, message: "Dữ liệu từng dòng hợp lệ" }
    : { success: false, message: errors.slice(0, 10).join("; ") + (errors.length > 10 ? `; và ${errors.length - 10} lỗi khác` : "") };
}

/**
 * Với cùng một hóa đơn xuất hiện ở cả báo cáo cũ và mới, giá trị cột K của
 * báo cáo mới chỉ được tăng hoặc giữ nguyên, không được giảm.
 */
function compareOpeningCumulativeAmounts_(dataOld, dataNew) {
  const oldAmounts = extractOpeningCumulativeAmounts_(dataOld);
  const newAmounts = extractOpeningCumulativeAmounts_(dataNew);
  const errors = [];

  Object.keys(oldAmounts).forEach(function(key) {
    if (!Object.prototype.hasOwnProperty.call(newAmounts, key)) return;
    if (newAmounts[key].amount < oldAmounts[key].amount) {
      errors.push(`${oldAmounts[key].label}: lũy kế đầu kỳ file mới ${newAmounts[key].amount.toLocaleString("vi-VN")} nhỏ hơn file cũ ${oldAmounts[key].amount.toLocaleString("vi-VN")}`);
    }
  });

  return errors.length === 0
    ? { success: true, message: "Lũy kế đầu kỳ không giảm" }
    : { success: false, message: errors.slice(0, 10).join("; ") + (errors.length > 10 ? `; và ${errors.length - 10} lỗi khác` : "") };
}

function extractOpeningCumulativeAmounts_(matrixData) {
  const result = {};
  let currentSection = "";
  for (let r = 0; r < matrixData.length; r++) {
    const row = matrixData[r];
    const section = getCrossCheckSectionCode_(row[0]);
    if (section) {
      currentSection = section;
      continue;
    }
    if (!currentSection) continue;
    if (!isCrossCheckInvoiceRow_(row)) continue;
    const key = crossCheckInvoiceIdentity_(row);
    if (!key) continue;
    // Nếu một hóa đơn vô tình xuất hiện nhiều dòng thì cộng các giá trị để
    // phép so sánh không phụ thuộc thứ tự dòng.
    if (!result[key]) {
      result[key] = { amount: 0, label: crossCheckInvoiceLabel_(row) };
    }
    result[key].amount += crossCheckNumber_(row[10]); // K
  }
  return result;
}

function isCrossCheckInvoiceRow_(row) {
  return String(row[1] === null || row[1] === undefined ? "" : row[1]).trim() !== ""
    || String(row[2] === null || row[2] === undefined ? "" : row[2]).trim() !== "";
}

function isCrossCheckBlank_(value) {
  return value === null || value === undefined || String(value).trim() === "";
}

function isCrossCheckNumericValue_(value, allowBlank) {
  if (isCrossCheckBlank_(value)) return !!allowBlank;
  if (typeof value === "number") return isFinite(value);
  let text = String(value).trim().replace(/\s/g, "").replace(/\./g, "").replace(/,/g, ".");
  text = text.replace(/[^0-9.-]/g, "");
  return text !== "" && text !== "-" && text !== "." && isFinite(Number(text));
}

function isCrossCheckValidDate_(value) {
  if (value instanceof Date) return !isNaN(value.getTime());
  const text = String(value === null || value === undefined ? "" : value).trim();
  const match = text.match(/^(\d{1,2})[\/-](\d{1,2})[\/-](\d{4})$/);
  if (!match) return false;
  const day = Number(match[1]);
  const month = Number(match[2]);
  const year = Number(match[3]);
  const parsed = new Date(year, month - 1, day);
  return parsed.getFullYear() === year
    && parsed.getMonth() === month - 1
    && parsed.getDate() === day;
}

function getCrossCheckSectionCode_(value) {
  const match = String(value === null || value === undefined ? "" : value).trim().match(/^([IVX]+)\./);
  return match ? match[1] : "";
}

function crossCheckInvoiceIdentity_(row) {
  const ncc = String(row[1] === null || row[1] === undefined ? "" : row[1]).trim();
  const invoiceNo = String(row[2] === null || row[2] === undefined ? "" : row[2]).trim();
  const dateText = crossCheckDateText_(row[3]);
  const vnd = crossCheckNumber_(row[4]);
  const usd = crossCheckNumber_(row[5]);
  return `${ncc}_${invoiceNo}_${dateText}_${vnd}_${usd}`;
}

function crossCheckInvoiceLabel_(row) {
  const ncc = String(row[1] === null || row[1] === undefined ? "" : row[1]).trim() || "không rõ NCC";
  const invoiceNo = String(row[2] === null || row[2] === undefined ? "" : row[2]).trim() || "không rõ số HĐ";
  return `NCC ${ncc}, HĐ ${invoiceNo}`;
}

function crossCheckDateText_(value) {
  return value instanceof Date
    ? Utilities.formatDate(value, "GMT+7", "dd/MM/yyyy")
    : String(value === null || value === undefined ? "" : value).trim();
}

function crossCheckNumber_(value) {
  if (typeof value === "number" && isFinite(value)) return value;
  let text = String(value === null || value === undefined ? "" : value).trim();
  if (!text) return 0;
  text = text.replace(/\s/g, "").replace(/\./g, "").replace(/,/g, ".").replace(/[^0-9.-]/g, "");
  const parsed = Number(text);
  return isFinite(parsed) ? parsed : 0;
}

function findValidCrossCheckSheets_(spreadsheet) {
  const REPORT_TITLE = "BIÊN BẢN BÁO CÁO HÓA ĐƠN CÔNG NỢ";
  let validSheets = [];

  spreadsheet.getSheets().forEach(function(sheetItem) {
    let titleA1 = String(sheetItem.getRange("A1").getValue()).trim();
    if (titleA1 === REPORT_TITLE) {
      validSheets.push({
        sheetObj: sheetItem,
        dot: String(sheetItem.getRange("D2").getValue()).trim(),
        thang: String(sheetItem.getRange("F2").getValue()).trim()
      });
    }
  });

  return validSheets;
}

function getCrossCheckTimeScore_(item) {
  const dotWeights = { "Đợt 1+2": 1, "Đợt 3+4": 2, "Đợt 5+6": 3, "Đợt 7": 4 };
  let monthNumber = parseInt(item.thang.replace(/[^0-9]/g, ""), 10) || 0;
  let dotWeight = dotWeights[item.dot] || 0;
  return (monthNumber * 10) + dotWeight;
}

function compareCrossCheckRecency_(left, right) {
  const dotWeights = { "Đợt 1+2": 1, "Đợt 3+4": 2, "Đợt 5+6": 3, "Đợt 7": 4 };
  const leftMonth = parseInt(String(left.thang).replace(/[^0-9]/g, ""), 10) || 0;
  const rightMonth = parseInt(String(right.thang).replace(/[^0-9]/g, ""), 10) || 0;
  if (leftMonth === rightMonth) {
    return (dotWeights[left.dot] || 0) - (dotWeights[right.dot] || 0);
  }
  if (leftMonth === 1 && rightMonth === 12) return 1;
  if (leftMonth === 12 && rightMonth === 1) return -1;
  return leftMonth - rightMonth;
}

function createCrossCheckResultRow_(employeeName, status, message, latestInfo) {
  const row = [employeeName, status, message];
  if (latestInfo) {
    row.crossCheckLatest = latestInfo;
  }
  return row;
}

function getNewestCrossCheckInfo_(validSheets) {
  let latest = null;
  validSheets.forEach(function(item) {
    const candidate = createCrossCheckLatestInfo_(item);
    if (!latest || compareCrossCheckRecency_(candidate, latest) > 0) {
      latest = candidate;
    }
  });
  return latest;
}

function createCrossCheckLatestInfo_(item) {
  return {
    dot: item.dot,
    thang: item.thang,
    score: getCrossCheckTimeScore_(item)
  };
}

function extractSpreadsheetIdFromValue_(rawValue) {
  let spreadsheetId = String(rawValue).trim();
  if (spreadsheetId.includes("docs.google.com/spreadsheets")) {
    let matches = spreadsheetId.match(/\/d\/([^/]+)/);
    if (matches && matches[1]) spreadsheetId = matches[1];
  }
  return spreadsheetId;
}

function getOrCreateCrossCheckReportSheet_(spreadsheet) {
  const REPORT_SHEET_NAME = "Ket qua kiem tra cheo";
  let reportSheet = spreadsheet.getSheetByName(REPORT_SHEET_NAME);
  if (!reportSheet) {
    reportSheet = spreadsheet.insertSheet(REPORT_SHEET_NAME);
  }
  ensureCrossCheckReportHeader_(reportSheet);
  return reportSheet;
}

function ensureCrossCheckReportHeader_(reportSheet) {
  const headers = [["Nhân viên phụ trách", "Trạng thái kiểm tra", "Chi tiết kết quả đối chiếu chéo"]];
  reportSheet.getRange(1, 1, 1, 3).setValues(headers).setFontWeight("bold");
}

function clearCrossCheckReportRows_(reportSheet) {
  let lastRowReportBefore = reportSheet.getLastRow();
  if (lastRowReportBefore >= 2) {
    let columnCount = Math.max(reportSheet.getLastColumn(), 3);
    reportSheet.getRange(2, 1, lastRowReportBefore - 1, columnCount).clearContent();
  }
}

function writeCrossCheckReportRows_(reportSheet, testResults) {
  if (!testResults || testResults.length === 0) return;

  const values = testResults.map(function(row) {
    return [row[0], row[1], row[2]];
  });
  reportSheet.getRange(2, 1, values.length, 3).setValues(values);
  writeCrossCheckSummaryRow_(reportSheet, values.length + 2, getLatestCrossCheckInfoFromRows_(testResults));
  applyCrossCheckReportFormatting_(reportSheet, values.length);
  reportSheet.autoResizeColumns(1, 3);
}

function writeCrossCheckSummaryRow_(reportSheet, rowNumber, latestInfo) {
  const summaryText = buildCrossCheckSummaryText_(latestInfo);
  reportSheet.getRange(rowNumber, 1, 1, 3).setValues([[summaryText, "", ""]]).setFontWeight("bold");
}

function buildCrossCheckSummaryText_(latestInfo) {
  const timeZone = Session.getScriptTimeZone() || "Asia/Ho_Chi_Minh";
  const dateText = Utilities.formatDate(new Date(), timeZone, "dd/MM/yyyy");
  if (!latestInfo || !latestInfo.dot || !latestInfo.thang) {
    return "Đã kiểm tra chéo gần nhất " + dateText + " chưa xác định đợt/tháng";
  }
  return "Đã kiểm tra chéo gần nhất " + dateText + " " + latestInfo.dot + " tháng " + formatCrossCheckMonth_(latestInfo.thang);
}

function formatCrossCheckMonth_(monthValue) {
  const monthText = String(monthValue).trim();
  const monthNumber = monthText.replace(/[^0-9]/g, "");
  return monthNumber || monthText;
}

function getLatestCrossCheckInfoFromRows_(rows) {
  let latest = null;
  rows.forEach(function(row) {
    if (!row.crossCheckLatest) return;
    if (!latest || compareCrossCheckRecency_(row.crossCheckLatest, latest) > 0) {
      latest = row.crossCheckLatest;
    }
  });
  return latest;
}

function applyCrossCheckReportFormatting_(reportSheet, resultCount) {
  if (resultCount <= 0) return;

  let statusRange = reportSheet.getRange(2, 2, resultCount, 1);
  let rulePassed = SpreadsheetApp.newConditionalFormatRule().whenTextEqualTo("PASSED").setBackground("#b7e1cd").setRanges([statusRange]).build();
  let ruleFailed = SpreadsheetApp.newConditionalFormatRule().whenTextContains("THẤT BẠI").setBackground("#f4c7c3").setRanges([statusRange]).build();
  let ruleError = SpreadsheetApp.newConditionalFormatRule().whenTextContains("LỖI").setBackground("#fce8b2").setRanges([statusRange]).build();

  reportSheet.setConditionalFormatRules([rulePassed, ruleFailed, ruleError]);
}

/**
 * Hàm bóc tách hóa đơn dựa vào các ký hiệu phần la mã mục tiêu
 * Đối chiếu chặt chẽ trên 5 thông tin: Cột B (NCC), Cột C (Số HĐ), Cột D (Ngày HĐ), Cột E (Trị giá VND), Cột F (Trị giá USD)
 */
function extractInvoicesBySections(matrixData, sectionHeaders) {
  let invoices = [];
  let isTargetSection = false;
  
  for (let r = 0; r < matrixData.length; r++) {
    let cellA = String(matrixData[r][0]).trim();
    
    // Bắt đầu vào phân vùng quét dữ liệu của mục tiêu
    if (sectionHeaders.some(h => cellA.startsWith(h))) { 
      isTargetSection = true; 
      continue; 
    }
    // Ra khỏi phân vùng khi chạm mục La Mã kế tiếp
    if (isTargetSection && cellA.match(/^[I|V|X]+\./)) { 
      isTargetSection = false; 
    }
    
    if (isTargetSection) {
      let ncc = String(matrixData[r][1]).trim();
      let invoiceNo = String(matrixData[r][2]).trim();
      let invoiceDate = matrixData[r][3];
      let valVnd = parseFloat(matrixData[r][4]) || 0;
      let valUsd = parseFloat(matrixData[r][5]) || 0;
      
      // Bỏ qua dòng trống, chỉ lấy dòng thực sự phát sinh hồ sơ
      if (invoiceNo !== "" || ncc !== "") {
        let dateStr = "";
        if (invoiceDate instanceof Date) {
          dateStr = Utilities.formatDate(invoiceDate, "GMT+7", "dd/MM/yyyy");
        } else {
          dateStr = String(invoiceDate).trim();
        }
        
        invoices.push({
          ncc: ncc,
          invoiceNo: invoiceNo,
          dateStr: dateStr,
          vnd: valVnd,
          usd: valUsd,
          // Chuỗi khóa kiểm tra định danh duy nhất (Tránh lệch dòng trống hay thứ tự sắp xếp)
          stringKey: `${ncc}_${invoiceNo}_${dateStr}_${valVnd}_${valUsd}`
        });
      }
    }
  }
  return invoices;
}

/**
 * Hàm thuật toán so sánh đối chiếu danh sách các cặp hóa đơn độc lập
 */
function compareInvoiceLists(listLeft, listRight) {
  if (listLeft.length !== listRight.length) {
    return { success: false, message: `Số lượng hóa đơn không khớp (Vế trái có ${listLeft.length}, Vế phải có ${listRight.length})` };
  }
  
  // Lập bản đồ băm đếm tần suất xuất hiện dữ liệu ở vế phải
  let rightMap = {};
  listRight.forEach(item => {
    rightMap[item.stringKey] = (rightMap[item.stringKey] || 0) + 1;
  });
  
  // Duyệt và triệt tiêu tần suất từ vế trái sang vế phải
  for (let i = 0; i < listLeft.length; i++) {
    let key = listLeft[i].stringKey;
    if (!rightMap[key] || rightMap[key] <= 0) {
      return { 
        success: false, 
        message: `Sai lệch hoặc thiếu: [NCC: ${listLeft[i].ncc} | Số HĐ: ${listLeft[i].invoiceNo} | Ngày: ${listLeft[i].dateStr} | VND: ${listLeft[i].vnd.toLocaleString()} | USD: ${listLeft[i].usd}]` 
      };
    } else {
      rightMap[key]--;
    }
  }
  
  return { success: true, message: "Khớp hoàn toàn" };
}
