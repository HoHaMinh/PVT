/**
 * PVT - phần bổ sung xuất cảnh báo công nợ dạng JSON cho Spring Boot.
 *
 * QUAN TRỌNG:
 * - Dán file này thành một file .gs MỚI trong project Apps Script hiện có.
 * - Không sửa masterCheckAllFilesAndSendEmail() và trigger gửi Gmail cũ.
 * - Các hàm ở đây không gửi email và không gọi hàm gửi email cũ.
 * - Script Properties bắt buộc:
 *     PVT_MASTER_SPREADSHEET_ID = ID file Danh sach nhan vien
 *     PVT_SHARED_TOKEN          = chuỗi bí mật giống application.properties của PVT
 * - Sheet "Danh sach nhan vien" dùng thêm cột D (từ hàng 4): Bravo ID/account_name.
 */

function doPost(e) {
  const timeZone = Session.getScriptTimeZone() || "Asia/Ho_Chi_Minh";
  const today = pvtStartOfDay_(new Date());

  try {
    const body = pvtParseRequest_(e);
    const expectedToken = PropertiesService.getScriptProperties().getProperty("PVT_SHARED_TOKEN") || "";

    if (!expectedToken || body.token !== expectedToken) {
      return pvtJsonOutput_({
        success: false,
        skipped: false,
        scanDate: Utilities.formatDate(today, timeZone, "yyyy-MM-dd"),
        generatedAt: pvtFormatDateTime_(new Date(), timeZone),
        message: "Không có quyền truy cập."
      });
    }
    if (body.action !== "scanDebtAlerts") {
      return pvtJsonOutput_({
        success: false,
        skipped: false,
        scanDate: Utilities.formatDate(today, timeZone, "yyyy-MM-dd"),
        generatedAt: pvtFormatDateTime_(new Date(), timeZone),
        message: "Action không hợp lệ."
      });
    }

    const lock = LockService.getScriptLock();
    if (!lock.tryLock(5000)) {
      return pvtJsonOutput_({
        success: false,
        skipped: false,
        scanDate: Utilities.formatDate(today, timeZone, "yyyy-MM-dd"),
        generatedAt: pvtFormatDateTime_(new Date(), timeZone),
        message: "Một lượt quét khác đang chạy, vui lòng thử lại sau."
      });
    }

    try {
      return pvtJsonOutput_(pvtCollectDebtAlerts_());
    } finally {
      lock.releaseLock();
    }
  } catch (error) {
    console.error("PVT debt Web App error: " + String(error));
    return pvtJsonOutput_({
      success: false,
      skipped: false,
      scanDate: Utilities.formatDate(today, timeZone, "yyyy-MM-dd"),
      generatedAt: pvtFormatDateTime_(new Date(), timeZone),
      message: "Lỗi Apps Script: " + pvtErrorMessage_(error)
    });
  }
}

/** Chạy thủ công hàm này một lần để cấp quyền và xem thử JSON trong Execution log. */
function pvtTestDebtAlertCollection() {
  const result = pvtCollectDebtAlerts_();
  console.log(JSON.stringify(result, null, 2));
  return result;
}

function pvtCollectDebtAlerts_() {
  const CONFIG_SHEET_NAME = "Danh sach nhan vien";
  const MASTER_ID_KEY = "PVT_MASTER_SPREADSHEET_ID";
  const timeZone = Session.getScriptTimeZone() || "Asia/Ho_Chi_Minh";
  const now = new Date();
  const today = pvtStartOfDay_(now);
  const baseResponse = {
    success: true,
    skipped: false,
    scanDate: Utilities.formatDate(today, timeZone, "yyyy-MM-dd"),
    generatedAt: pvtFormatDateTime_(now, timeZone),
    daysOffset: 7,
    message: "",
    skipReason: "",
    employees: []
  };

  const masterId = PropertiesService.getScriptProperties().getProperty(MASTER_ID_KEY);
  if (!masterId) {
    throw new Error("Thiếu Script Property " + MASTER_ID_KEY + ".");
  }

  const masterSpreadsheet = SpreadsheetApp.openById(masterId.trim());
  const configSheet = masterSpreadsheet.getSheetByName(CONFIG_SHEET_NAME);
  if (!configSheet) {
    throw new Error("Không tìm thấy sheet cấu hình: " + CONFIG_SHEET_NAME + ".");
  }

  let daysOffset = Number(configSheet.getRange("B1").getValue());
  if (!isFinite(daysOffset) || daysOffset < 0) {
    daysOffset = 7;
  }
  daysOffset = Math.floor(daysOffset);
  baseResponse.daysOffset = daysOffset;

  if (today.getDay() === 0) {
    baseResponse.skipped = true;
    baseResponse.skipReason = "Hôm nay là Chủ Nhật, hệ thống không quét công nợ.";
    baseResponse.message = baseResponse.skipReason;
    return baseResponse;
  }
  if (pvtIsHoliday_(today, masterSpreadsheet, timeZone)) {
    baseResponse.skipped = true;
    baseResponse.skipReason = "Hôm nay là ngày nghỉ Lễ/Tết, hệ thống không quét công nợ.";
    baseResponse.message = baseResponse.skipReason;
    return baseResponse;
  }

  const lastRow = configSheet.getLastRow();
  if (lastRow < 4) {
    baseResponse.message = "Sheet cấu hình chưa có nhân viên từ hàng 4.";
    return baseResponse;
  }

  // A: họ tên, B: ID/URL file, C: email cũ, D: Bravo ID/account_name của PVT.
  // Cột C vẫn được hàm gửi Gmail cũ sử dụng; phần PVT không thay đổi cột này.
  const configRows = configSheet.getRange(4, 1, lastRow - 3, 4).getValues();
  const targetDate = pvtStartOfDay_(today);
  targetDate.setDate(targetDate.getDate() + daysOffset);
  targetDate.setHours(23, 59, 59, 999);

  configRows.forEach(function (row) {
    const employeeName = pvtText_(row[0]);
    const rawSpreadsheetId = pvtText_(row[1]);
    const accountName = pvtText_(row[3]);

    // Bỏ qua hoàn toàn hàng trống để tránh sinh dòng rác ở PVT.
    if (!employeeName && !rawSpreadsheetId && !accountName) {
      return;
    }

    const employeeResult = {
      accountName: accountName,
      employeeName: employeeName || accountName || "Không rõ nhân viên",
      spreadsheetId: "",
      spreadsheetUrl: "",
      reportSheetName: "",
      reportBatch: "",
      reportMonth: "",
      status: "OK",
      message: "",
      alerts: [],
      importAlerts: []
    };

    if (!rawSpreadsheetId) {
      employeeResult.status = "MISSING_CONFIG";
      employeeResult.message = "Thiếu ID/URL Google Sheets tại cột B.";
      baseResponse.employees.push(employeeResult);
      return;
    }
    if (!accountName) {
      employeeResult.status = "MISSING_ACCOUNT";
      employeeResult.message = "Thiếu Bravo ID/account_name tại cột D.";
    }

    try {
      const spreadsheetId = pvtExtractSpreadsheetId_(rawSpreadsheetId);
      employeeResult.spreadsheetId = spreadsheetId;
      employeeResult.spreadsheetUrl = "https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/edit";

      const employeeSpreadsheet = SpreadsheetApp.openById(spreadsheetId);
      const reports = pvtFindReportSheets_(employeeSpreadsheet.getSheets());
      const duplicate = pvtFindDuplicateReport_(reports);
      if (duplicate) {
        employeeResult.status = "DUPLICATE";
        employeeResult.message = "Có sheet trùng " + duplicate + ", chưa thể xác định báo cáo mới nhất.";
        baseResponse.employees.push(employeeResult);
        return;
      }

      const newest = pvtFindNewestReport_(reports);
      if (!newest) {
        employeeResult.status = "NO_REPORT";
        employeeResult.message = "Không tìm thấy sheet BIÊN BẢN BÁO CÁO HÓA ĐƠN CÔNG NỢ hợp lệ.";
        baseResponse.employees.push(employeeResult);
        return;
      }

      employeeResult.reportSheetName = newest.sheet.getName();
      employeeResult.reportBatch = newest.batch;
      employeeResult.reportMonth = newest.month;
      employeeResult.alerts = pvtScanReport_(newest.sheet, today, targetDate, timeZone);
      employeeResult.importAlerts = pvtScanImportAlerts_(
        newest.sheet, today, timeZone, spreadsheetId
      );
      if (!employeeResult.message) {
        employeeResult.message = employeeResult.alerts.length > 0
          ? "Có " + employeeResult.alerts.length + " hồ sơ cần chú ý."
          : "Không có hồ sơ công nợ cần cảnh báo.";
      }
    } catch (error) {
      employeeResult.status = "ERROR";
      employeeResult.message = pvtErrorMessage_(error);
    }

    baseResponse.employees.push(employeeResult);
  });

  const totalAlerts = baseResponse.employees.reduce(function (sum, employee) {
    return sum + (employee.alerts ? employee.alerts.length : 0);
  }, 0);
  const totalImportAlerts = baseResponse.employees.reduce(function (sum, employee) {
    return sum + (employee.importAlerts ? employee.importAlerts.length : 0);
  }, 0);
  baseResponse.message = "Đã đọc " + baseResponse.employees.length
    + " nhân viên; có " + totalAlerts + " hồ sơ công nợ cần chú ý"
    + " và " + totalImportAlerts + " hồ sơ NK cần báo kế toán.";
  return baseResponse;
}

function pvtFindReportSheets_(sheets) {
  const REPORT_TITLE = "BIÊN BẢN BÁO CÁO HÓA ĐƠN CÔNG NỢ";
  const reports = [];
  sheets.forEach(function (sheet) {
    if (pvtText_(sheet.getRange("A1").getValue()) === REPORT_TITLE) {
      reports.push({
        sheet: sheet,
        batch: pvtText_(sheet.getRange("D2").getValue()),
        month: pvtText_(sheet.getRange("F2").getValue())
      });
    }
  });
  return reports;
}

function pvtFindDuplicateReport_(reports) {
  const seen = {};
  for (let i = 0; i < reports.length; i++) {
    const key = reports[i].batch + "\u0000" + reports[i].month;
    if (seen[key]) {
      return ("ĐỢT " + reports[i].batch + " THÁNG " + reports[i].month).toUpperCase();
    }
    seen[key] = true;
  }
  return "";
}

function pvtFindNewestReport_(reports) {
  const batchWeights = {
    "Đợt 1+2": 1,
    "Đợt 3+4": 2,
    "Đợt 5+6": 3,
    "Đợt 7": 4
  };
  let newest = null;
  let maxValue = -1;

  reports.forEach(function (report) {
    const monthNumber = parseInt(report.month.replace(/[^0-9]/g, ""), 10) || 0;
    const value = monthNumber * 10 + (batchWeights[report.batch] || 0);
    if (value > maxValue) {
      maxValue = value;
      newest = report;
    }
  });
  return newest;
}

function pvtScanReport_(sheet, today, targetDate, timeZone) {
  const lastRow = sheet.getLastRow();
  if (lastRow < 3) {
    return [];
  }

  const rows = sheet.getRange(2, 1, lastRow - 1, 13).getValues();
  const alerts = [];
  let currentSection = "";
  const millisecondsPerDay = 24 * 60 * 60 * 1000;

  rows.forEach(function (row) {
    const columnA = pvtText_(row[0]);
    if (columnA.indexOf("II.") === 0 || columnA.indexOf("III.") === 0) {
      currentSection = columnA;
      return;
    }
    if (/^[IVX]+\./.test(columnA)) {
      currentSection = "";
      return;
    }
    if (!(currentSection.indexOf("II.") === 0 || currentSection.indexOf("III.") === 0)) {
      return;
    }

    const rawDueDate = row[7];
    if (!(rawDueDate instanceof Date) || isNaN(rawDueDate.getTime())) {
      return;
    }
    const dueDate = pvtStartOfDay_(rawDueDate);
    if (dueDate > targetDate) {
      return;
    }

    const tPlus = Math.round((today.getTime() - dueDate.getTime()) / millisecondsPerDay);
    alerts.push({
      section: currentSection.split(".")[0] + ".",
      itemOrder: pvtText_(row[0]),
      supplier: pvtText_(row[1]),
      invoiceNumber: pvtText_(row[2]),
      amountDisplay: pvtAmount_(row[4]),
      dueDate: Utilities.formatDate(dueDate, timeZone, "yyyy-MM-dd"),
      tPlus: tPlus
    });
  });

  alerts.sort(function (a, b) { return b.tPlus - a.tPlus; });
  return alerts;
}

/**
 * Tìm hồ sơ nhập khẩu còn tiền phải thanh toán và đã bước vào khoảng cảnh
 * báo 10 ngày trước hạn báo cáo NHNN.
 *
 * Hạn của một hồ sơ là ngày đến trước trong hai mốc còn dữ liệu:
 * - Ngày vận đơn + 90 ngày (cột P).
 * - Ngày tờ khai + 45 ngày (cột Q).
 */
function pvtScanImportAlerts_(sheet, today, timeZone, spreadsheetId) {
  const lastRow = sheet.getLastRow();
  if (lastRow < 4) return [];

  const rows = sheet.getRange(2, 1, lastRow - 1, 17).getValues();
  const alerts = [];
  const millisecondsPerDay = 24 * 60 * 60 * 1000;
  let currentSection = "";

  rows.forEach(function(row) {
    const columnA = pvtText_(row[0]);
    const sectionMatch = columnA.match(/^([IVX]+)\./);
    if (sectionMatch) {
      currentSection = sectionMatch[1] + ".";
      return;
    }

    const supplier = pvtText_(row[1]);
    const invoiceNumber = pvtText_(row[2]);
    if (!supplier && !invoiceNumber) return;

    const domesticOrImport = pvtText_(row[14]).toUpperCase().replace(/\s/g, "");
    const remainingAmount = pvtNumericAmount_(row[12]);
    if (domesticOrImport !== "NK" || remainingAmount <= 0) return;

    const billOfLadingDate = pvtDateValue_(row[15]);
    const customsDeclarationDate = pvtDateValue_(row[16]);
    const deadlineCandidates = [];
    if (billOfLadingDate) deadlineCandidates.push(pvtAddDays_(billOfLadingDate, 90));
    if (customsDeclarationDate) deadlineCandidates.push(pvtAddDays_(customsDeclarationDate, 45));
    // Chưa có cả hai ngày thì chưa thể tính đúng hạn pháp lý; không sinh một
    // hạn giả. Dữ liệu sẽ được xét lại ở lần quét sau khi ngày được bổ sung.
    if (deadlineCandidates.length === 0) return;

    let regulatoryDeadline = deadlineCandidates[0];
    for (let i = 1; i < deadlineCandidates.length; i++) {
      if (deadlineCandidates[i] < regulatoryDeadline) regulatoryDeadline = deadlineCandidates[i];
    }
    regulatoryDeadline = pvtStartOfDay_(regulatoryDeadline);
    const daysRemaining = Math.round(
      (regulatoryDeadline.getTime() - pvtStartOfDay_(today).getTime()) / millisecondsPerDay
    );
    if (daysRemaining > 10) return;

    const invoiceDate = pvtDateValue_(row[3]);
    alerts.push({
      alertKey: pvtImportAlertKey_(spreadsheetId, supplier, invoiceNumber, invoiceDate, timeZone),
      section: currentSection,
      itemOrder: pvtText_(row[0]),
      supplier: supplier,
      invoiceNumber: invoiceNumber,
      remainingAmountDisplay: pvtAmount_(row[12]),
      billOfLadingDate: pvtFormatOptionalDate_(billOfLadingDate, timeZone),
      customsDeclarationDate: pvtFormatOptionalDate_(customsDeclarationDate, timeZone),
      regulatoryDeadline: Utilities.formatDate(regulatoryDeadline, timeZone, "yyyy-MM-dd"),
      daysRemaining: daysRemaining
    });
  });

  alerts.sort(function(a, b) { return a.daysRemaining - b.daysRemaining; });
  return alerts;
}

function pvtImportAlertKey_(spreadsheetId, supplier, invoiceNumber, invoiceDate, timeZone) {
  const source = [
    pvtText_(spreadsheetId).toLowerCase(),
    pvtText_(supplier).toLowerCase(),
    pvtText_(invoiceNumber).toLowerCase(),
    pvtFormatOptionalDate_(invoiceDate, timeZone)
  ].join("\u001f");
  const digest = Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, source, Utilities.Charset.UTF_8);
  return digest.map(function(value) {
    const normalized = value < 0 ? value + 256 : value;
    return ("0" + normalized.toString(16)).slice(-2);
  }).join("");
}

function pvtDateValue_(value) {
  if (value instanceof Date && !isNaN(value.getTime())) return pvtStartOfDay_(value);
  const text = pvtText_(value);
  if (!text) return null;
  const match = text.match(/^(\d{1,2})[\/-](\d{1,2})[\/-](\d{4})$/);
  if (!match) return null;
  const parsed = new Date(Number(match[3]), Number(match[2]) - 1, Number(match[1]));
  return isNaN(parsed.getTime()) ? null : pvtStartOfDay_(parsed);
}

function pvtAddDays_(value, days) {
  const result = pvtStartOfDay_(value);
  result.setDate(result.getDate() + days);
  return result;
}

function pvtFormatOptionalDate_(value, timeZone) {
  return value instanceof Date && !isNaN(value.getTime())
    ? Utilities.formatDate(value, timeZone, "yyyy-MM-dd")
    : "";
}

function pvtNumericAmount_(value) {
  if (typeof value === "number" && isFinite(value)) return value;
  let text = pvtText_(value);
  if (!text) return 0;
  text = text.replace(/\s/g, "").replace(/\./g, "").replace(/,/g, ".").replace(/[^0-9.-]/g, "");
  const parsed = Number(text);
  return isFinite(parsed) ? parsed : 0;
}

function pvtIsHoliday_(checkDate, masterSpreadsheet, timeZone) {
  const holidaySheet = masterSpreadsheet.getSheetByName("Lich nghi le");
  if (!holidaySheet || holidaySheet.getLastRow() < 1) {
    return false;
  }
  const checkDateText = Utilities.formatDate(checkDate, timeZone, "yyyy-MM-dd");
  const values = holidaySheet.getRange(1, 1, holidaySheet.getLastRow(), 1).getValues();
  return values.some(function (row) {
    const value = row[0];
    return value instanceof Date
      && Utilities.formatDate(value, timeZone, "yyyy-MM-dd") === checkDateText;
  });
}

function pvtParseRequest_(e) {
  if (!e || !e.postData || !e.postData.contents) {
    throw new Error("Request body trống.");
  }
  try {
    return JSON.parse(e.postData.contents);
  } catch (error) {
    throw new Error("Request JSON không hợp lệ.");
  }
}

function pvtExtractSpreadsheetId_(rawValue) {
  const value = pvtText_(rawValue);
  const match = value.match(/\/d\/([^/]+)/);
  const id = match && match[1] ? match[1] : value;
  if (!/^[a-zA-Z0-9_-]{20,}$/.test(id)) {
    throw new Error("ID/URL Google Sheets không hợp lệ.");
  }
  return id;
}

function pvtAmount_(value) {
  if (typeof value === "number" && isFinite(value)) {
    return value.toLocaleString("vi-VN");
  }
  return pvtText_(value);
}

function pvtText_(value) {
  return value === null || value === undefined ? "" : String(value).trim();
}

function pvtStartOfDay_(value) {
  const date = new Date(value);
  date.setHours(0, 0, 0, 0);
  return date;
}

function pvtFormatDateTime_(date, timeZone) {
  return Utilities.formatDate(date, timeZone, "yyyy-MM-dd'T'HH:mm:ssXXX");
}

function pvtErrorMessage_(error) {
  const message = error && error.message ? error.message : String(error || "Không rõ lỗi");
  return message.length > 900 ? message.substring(0, 900) : message;
}

function pvtJsonOutput_(value) {
  return ContentService
    .createTextOutput(JSON.stringify(value))
    .setMimeType(ContentService.MimeType.JSON);
}
