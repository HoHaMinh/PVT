const NEXT_DEBT_REPORT_CONFIG_ = {
  REPORT_TITLE: "BIÊN BẢN BÁO CÁO HÓA ĐƠN CÔNG NỢ",
  PERIOD_CELL: "D2",
  MONTH_CELL: "F2",
  DATA_COLUMN_COUNT: 17,
  REQUIRED_SECTIONS: ["I", "II", "III", "IV", "V"],
  SECTIONS_TO_CLEAR: ["I", "III", "IV", "V"],
  PERIOD_WEIGHTS: { "1+2": 1, "3+4": 2, "5+6": 3, "7": 4 }
};

/**
 * Menu Công nợ > Tự động tạo file đợt mới.
 *
 * Sau khi hoàn thành, file chỉ còn hai sheet báo cáo công nợ:
 * - sheet nguồn là đợt gần nhất, được giữ nguyên hoàn toàn;
 * - sheet mới được tạo từ sheet nguồn và chuyển sang đợt tiếp theo.
 */
function createNextDebtReportSheet() {
  const ui = SpreadsheetApp.getUi();
  const lock = LockService.getDocumentLock();

  if (!lock.tryLock(3000)) {
    ui.alert("Đang có một thao tác tạo đợt mới khác chạy. Vui lòng thử lại sau.");
    return;
  }

  try {
    createNextDebtReportSheetInternal_(SpreadsheetApp.getActiveSpreadsheet(), ui);
  } catch (error) {
    ui.alert(
      "Không tạo được file đợt mới",
      nextDebtReportErrorMessage_(error),
      ui.ButtonSet.OK
    );
  } finally {
    lock.releaseLock();
  }
}

function createNextDebtReportSheetInternal_(spreadsheet, ui) {
  const reportSheets = findDebtReportSheetsForNextPeriod_(spreadsheet);
  if (reportSheets.length === 0) {
    throw new Error("Không tìm thấy sheet có dòng đầu tiên là \""
      + NEXT_DEBT_REPORT_CONFIG_.REPORT_TITLE + "\".");
  }

  validateDebtReportPeriods_(reportSheets);
  const newest = findNewestDebtReport_(reportSheets);
  const nextPeriod = chooseNextDebtReportPeriod_(newest, ui);
  if (!nextPeriod) return;

  const sheetsToDelete = reportSheets.filter(function(item) {
    return item.sheet.getSheetId() !== newest.sheet.getSheetId();
  });
  const confirmation = [
    "Sheet nguồn giữ nguyên: " + newest.sheet.getName(),
    "Đợt nguồn: Đợt " + newest.periodCode + " - tháng " + newest.month,
    "Sheet mới: Đợt " + nextPeriod.periodCode + " - tháng " + nextPeriod.month,
    "Số sheet báo cáo cũ sẽ xóa: " + sheetsToDelete.length,
    "",
    "Tiếp tục thực hiện?"
  ].join("\n");

  if (ui.alert("Xác nhận tạo đợt công nợ mới", confirmation,
      ui.ButtonSet.YES_NO) !== ui.Button.YES) {
    return;
  }

  let newSheet = null;
  try {
    newSheet = newest.sheet.copyTo(spreadsheet);
    newSheet.setName(buildNextDebtReportSheetName_(spreadsheet, nextPeriod));
    newSheet.getRange(NEXT_DEBT_REPORT_CONFIG_.PERIOD_CELL)
      .setValue("Đợt " + nextPeriod.periodCode);
    newSheet.getRange(NEXT_DEBT_REPORT_CONFIG_.MONTH_CELL)
      .setValue(nextPeriod.month);

    prepareNextDebtReportSheet_(newSheet);
    SpreadsheetApp.flush();
  } catch (error) {
    if (newSheet && spreadsheet.getSheets().length > 1) {
      try {
        spreadsheet.deleteSheet(newSheet);
      } catch (cleanupError) {
        // Sheet nguồn chưa bao giờ bị sửa; giữ lỗi gốc để người dùng xử lý.
      }
    }
    throw error;
  }

  const deleteErrors = [];
  sheetsToDelete.forEach(function(item) {
    try {
      spreadsheet.deleteSheet(item.sheet);
    } catch (error) {
      deleteErrors.push(item.sheet.getName() + ": " + nextDebtReportErrorMessage_(error));
    }
  });

  spreadsheet.setActiveSheet(newSheet);
  spreadsheet.moveActiveSheet(newest.sheet.getIndex() + 1);
  SpreadsheetApp.flush();

  if (deleteErrors.length > 0) {
    ui.alert(
      "Đã tạo sheet mới nhưng chưa xóa hết sheet cũ",
      "Sheet mới: " + newSheet.getName() + "\n" + deleteErrors.join("\n"),
      ui.ButtonSet.OK
    );
    return;
  }

  spreadsheet.toast(
    "Đã tạo " + newSheet.getName() + " từ " + newest.sheet.getName()
      + "; sheet nguồn được giữ nguyên.",
    "Công nợ",
    8
  );
  ui.alert(
    "Tạo đợt mới thành công",
    "Đã tạo " + newSheet.getName() + " (Đợt " + nextPeriod.periodCode
      + " - tháng " + nextPeriod.month + ").\n"
      + "Đã chuyển dữ liệu mục III cũ vào mục II mới và làm trống các mục I, III, IV, V.",
    ui.ButtonSet.OK
  );
}

function findDebtReportSheetsForNextPeriod_(spreadsheet) {
  const reports = [];
  spreadsheet.getSheets().forEach(function(sheet) {
    const title = String(sheet.getRange("A1").getDisplayValue()).trim();
    if (title !== NEXT_DEBT_REPORT_CONFIG_.REPORT_TITLE) return;

    reports.push({
      sheet: sheet,
      periodText: String(sheet.getRange(NEXT_DEBT_REPORT_CONFIG_.PERIOD_CELL)
        .getDisplayValue()).trim(),
      periodCode: normalizeDebtReportPeriod_(sheet.getRange(
        NEXT_DEBT_REPORT_CONFIG_.PERIOD_CELL).getDisplayValue()),
      month: parseDebtReportMonth_(sheet.getRange(
        NEXT_DEBT_REPORT_CONFIG_.MONTH_CELL).getDisplayValue())
    });
  });
  return reports;
}

function validateDebtReportPeriods_(reportSheets) {
  const invalid = [];
  const seen = {};
  const duplicates = [];

  reportSheets.forEach(function(item) {
    if (!item.periodCode || !item.month) {
      invalid.push(item.sheet.getName() + " (D2: " + (item.periodText || "trống")
        + ", F2: " + item.sheet.getRange(NEXT_DEBT_REPORT_CONFIG_.MONTH_CELL)
          .getDisplayValue() + ")");
      return;
    }
    const key = item.month + "|" + item.periodCode;
    if (seen[key]) {
      duplicates.push("Đợt " + item.periodCode + " tháng " + item.month
        + " ở sheet " + seen[key] + " và " + item.sheet.getName());
    } else {
      seen[key] = item.sheet.getName();
    }
  });

  if (invalid.length > 0) {
    throw new Error("Các sheet sau có Đợt báo cáo hoặc Tháng không hợp lệ: "
      + invalid.join("; "));
  }
  if (duplicates.length > 0) {
    throw new Error("Đang có báo cáo trùng đợt và tháng: " + duplicates.join("; "));
  }
}

function findNewestDebtReport_(reportSheets) {
  return reportSheets.reduce(function(newest, item) {
    return !newest || compareDebtReportRecency_(item, newest) > 0
      ? item : newest;
  }, null);
}

function compareDebtReportRecency_(left, right) {
  if (left.month === right.month) {
    return NEXT_DEBT_REPORT_CONFIG_.PERIOD_WEIGHTS[left.periodCode]
      - NEXT_DEBT_REPORT_CONFIG_.PERIOD_WEIGHTS[right.periodCode];
  }
  // Biểu mẫu không có ô năm. Xử lý riêng vòng chuyển năm để tháng 1 được xem
  // là mới hơn tháng 12; các tháng còn lại giữ đúng quy tắc quét hiện hành.
  if (left.month === 1 && right.month === 12) return 1;
  if (left.month === 12 && right.month === 1) return -1;
  return left.month - right.month;
}

function chooseNextDebtReportPeriod_(newest, ui) {
  if (newest.periodCode !== "5+6") {
    return calculateNextDebtReportPeriod_(newest.periodCode, newest.month, false);
  }

  const selection = ui.alert(
    "Sau Đợt 5+6 sẽ tạo đợt nào?",
    "Chọn CÓ nếu tháng " + newest.month + " có Đợt 7.\n"
      + "Chọn KHÔNG nếu bỏ qua Đợt 7 và tạo Đợt 1+2 của tháng kế tiếp.\n"
      + "Chọn HỦY để không thay đổi dữ liệu.",
    ui.ButtonSet.YES_NO_CANCEL
  );
  if (selection === ui.Button.CANCEL || selection === ui.Button.CLOSE) return null;
  return calculateNextDebtReportPeriod_(newest.periodCode, newest.month,
    selection === ui.Button.YES);
}

function calculateNextDebtReportPeriod_(periodCode, month, usePeriod7) {
  const normalizedPeriod = normalizeDebtReportPeriod_(periodCode);
  const normalizedMonth = parseDebtReportMonth_(month);
  if (!normalizedPeriod || !normalizedMonth) {
    throw new Error("Không xác định được Đợt báo cáo hoặc Tháng của sheet gần nhất.");
  }

  if (normalizedPeriod === "1+2") {
    return { periodCode: "3+4", month: normalizedMonth };
  }
  if (normalizedPeriod === "3+4") {
    return { periodCode: "5+6", month: normalizedMonth };
  }
  if (normalizedPeriod === "5+6" && usePeriod7) {
    return { periodCode: "7", month: normalizedMonth };
  }
  if (normalizedPeriod === "5+6" || normalizedPeriod === "7") {
    return { periodCode: "1+2", month: normalizedMonth === 12 ? 1 : normalizedMonth + 1 };
  }
  throw new Error("Đợt báo cáo không thuộc các lựa chọn 1+2, 3+4, 5+6 hoặc 7.");
}

function prepareNextDebtReportSheet_(sheet) {
  let sections = getDebtReportSections_(sheet);
  validateRequiredDebtReportSections_(sections);

  let sourceRows = getDebtReportInvoiceRows_(sheet, sections.III);
  let destinationRows = getDebtReportBlankRows_(sheet, sections.II);
  const extraRows = Math.max(0, sourceRows.length - destinationRows.length);

  if (extraRows > 0) {
    sheet.insertRowsBefore(sections.III.headerRow, extraRows);
    sections = getDebtReportSections_(sheet);
    validateRequiredDebtReportSections_(sections);
    sourceRows = getDebtReportInvoiceRows_(sheet, sections.III);
    destinationRows = getDebtReportBlankRows_(sheet, sections.II);
  }

  if (destinationRows.length < sourceRows.length) {
    throw new Error("Không thể tạo đủ dòng ở mục II để nhận dữ liệu từ mục III.");
  }

  const columnCount = Math.max(NEXT_DEBT_REPORT_CONFIG_.DATA_COLUMN_COUNT,
    sheet.getLastColumn());
  sourceRows.forEach(function(sourceRow, index) {
    const destinationRow = destinationRows[index];
    sheet.getRange(sourceRow, 1, 1, columnCount)
      .copyTo(sheet.getRange(destinationRow, 1, 1, columnCount));
    sheet.setRowHeight(destinationRow, sheet.getRowHeight(sourceRow));
  });

  // Sau khi copy xong mới xóa nội dung các phần phải làm trống. clearContent
  // giữ nguyên số dòng, định dạng, đường viền và data validation của biểu mẫu.
  sections = getDebtReportSections_(sheet);
  NEXT_DEBT_REPORT_CONFIG_.SECTIONS_TO_CLEAR.forEach(function(sectionCode) {
    clearDebtReportSectionContent_(sheet, sections[sectionCode], columnCount);
  });
}

function getDebtReportSections_(sheet) {
  const lastRow = Math.max(1, sheet.getLastRow());
  const columnValues = sheet.getRange(1, 1, lastRow, 1).getDisplayValues();
  const headers = [];

  columnValues.forEach(function(row, index) {
    const match = String(row[0]).trim().match(/^([IVX]+)\./);
    if (match) headers.push({ code: match[1], headerRow: index + 1 });
  });

  const sections = {};
  headers.forEach(function(header, index) {
    if (sections[header.code]) {
      throw new Error("Mục " + header.code + " xuất hiện nhiều lần trong sheet "
        + sheet.getName() + ".");
    }
    sections[header.code] = {
      code: header.code,
      headerRow: header.headerRow,
      startRow: header.headerRow + 1,
      endRow: index + 1 < headers.length ? headers[index + 1].headerRow - 1 : lastRow
    };
  });
  return sections;
}

function validateRequiredDebtReportSections_(sections) {
  const missing = NEXT_DEBT_REPORT_CONFIG_.REQUIRED_SECTIONS.filter(function(code) {
    return !sections[code];
  });
  if (missing.length > 0) {
    throw new Error("Không tìm thấy tiêu đề mục " + missing.join(", ")
      + ". Tiêu đề phải bắt đầu bằng số La Mã và dấu chấm, ví dụ: II.");
  }
}

function getDebtReportInvoiceRows_(sheet, section) {
  if (!section || section.endRow < section.startRow) return [];
  const values = sheet.getRange(section.startRow, 2,
    section.endRow - section.startRow + 1, 2).getDisplayValues();
  const rows = [];
  values.forEach(function(row, index) {
    if (String(row[0]).trim() || String(row[1]).trim()) {
      rows.push(section.startRow + index);
    }
  });
  return rows;
}

function getDebtReportBlankRows_(sheet, section) {
  if (!section || section.endRow < section.startRow) return [];
  const values = sheet.getRange(section.startRow, 2,
    section.endRow - section.startRow + 1, 2).getDisplayValues();
  const rows = [];
  values.forEach(function(row, index) {
    if (!String(row[0]).trim() && !String(row[1]).trim()) {
      rows.push(section.startRow + index);
    }
  });
  return rows;
}

function clearDebtReportSectionContent_(sheet, section, columnCount) {
  if (!section || section.endRow < section.startRow) return;
  sheet.getRange(section.startRow, 1,
    section.endRow - section.startRow + 1, columnCount).clearContent();
}

function normalizeDebtReportPeriod_(value) {
  const match = String(value === null || value === undefined ? "" : value)
    .trim().replace(/\s/g, "").match(/(?:Đợt)?(1\+2|3\+4|5\+6|7)$/i);
  return match ? match[1] : "";
}

function parseDebtReportMonth_(value) {
  const match = String(value === null || value === undefined ? "" : value)
    .trim().match(/(?:tháng\s*)?(\d{1,2})/i);
  if (!match) return 0;
  const month = Number(match[1]);
  return month >= 1 && month <= 12 ? month : 0;
}

function buildNextDebtReportSheetName_(spreadsheet, nextPeriod) {
  const numericNames = spreadsheet.getSheets().map(function(sheet) {
    const name = sheet.getName().trim();
    return /^\d+$/.test(name) ? Number(name) : 0;
  });
  let nextNumber = Math.max.apply(null, [0].concat(numericNames)) + 1;
  while (spreadsheet.getSheetByName(String(nextNumber))) nextNumber++;
  if (numericNames.some(function(number) { return number > 0; })) return String(nextNumber);

  const baseName = "Đợt " + nextPeriod.periodCode + " T" + nextPeriod.month;
  let candidate = baseName;
  let suffix = 2;
  while (spreadsheet.getSheetByName(candidate)) {
    candidate = baseName + " (" + suffix + ")";
    suffix++;
  }
  return candidate;
}

function nextDebtReportErrorMessage_(error) {
  const message = error && error.message ? error.message : String(error || "Không rõ lỗi");
  return message.length > 1500 ? message.substring(0, 1500) : message;
}
