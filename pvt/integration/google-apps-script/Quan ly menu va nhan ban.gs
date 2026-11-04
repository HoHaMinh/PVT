const EMPLOYEE_PROVISIONING_CONFIG_ = {
  CONFIG_SHEET_NAME: "Danh sach nhan vien",
  START_ROW: 4,
  COL_EMPLOYEE_NAME: 1,
  COL_SPREADSHEET_URL: 2,
  COL_PRIMARY_EDITOR_EMAILS: 5,
  COL_REVIEWER_EMAILS: 6,
  TEMPLATE_ID_PROPERTY: "EMPLOYEE_TEMPLATE_SPREADSHEET_ID",
  OUTPUT_FOLDER_ID_PROPERTY: "EMPLOYEE_OUTPUT_FOLDER_ID",
  FILE_NAME_PREFIX: "Công nợ - "
};

/**
 * Menu dùng chung:
 * - File nhân viên: chỉ hiện mục chạy kiểm tra chéo file hiện tại.
 * - File trung tâm: hiện thêm các mục chạy tổng hợp và nhân bản file user.
 */
function onOpen() {
  const ui = SpreadsheetApp.getUi();
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const menu = ui.createMenu("Công nợ");
  const isMasterFile = !!ss.getSheetByName(EMPLOYEE_PROVISIONING_CONFIG_.CONFIG_SHEET_NAME);

  menu.addItem("Chạy kiểm tra chéo file này", "runCrossCheckCurrentFile");
  if (!isMasterFile) {
    menu.addItem("Tự động tạo file đợt mới", "createNextDebtReportSheet");
  }

  if (isMasterFile) {
    menu.addSeparator();
    menu.addItem("Chạy kiểm tra chéo tất cả nhân viên", "runCrossCheckValidationTest");
    menu.addSeparator();
    menu.addItem("Thiết lập cột nhân bản", "setupEmployeeProvisioningColumns");
    menu.addItem("Cài ID file mẫu nhân viên", "setEmployeeTemplateSpreadsheetId");
    menu.addItem("Cài thư mục lưu file nhân viên", "setEmployeeOutputFolderId");
    menu.addSeparator();
    menu.addItem("Tạo file cho dòng đang chọn", "createEmployeeFilesForSelectedRows");
    menu.addItem("Tạo file cho các dòng chưa có link", "createEmployeeFilesForRowsMissingLink");
    menu.addItem("Cập nhật quyền chia sẻ dòng đang chọn", "syncEmployeeSharingForSelectedRows");
  }

  menu.addToUi();
}

function setupEmployeeProvisioningColumns() {
  const sheet = requireEmployeeConfigSheet_();
  ensureEmployeeProvisioningColumns_(sheet);
  SpreadsheetApp.getActiveSpreadsheet().toast("Đã thiết lập cột E/F cho người điền chính và người review.", "Công nợ", 5);
}

function setEmployeeTemplateSpreadsheetId() {
  const ui = SpreadsheetApp.getUi();
  const response = ui.prompt(
    "Cài ID file mẫu nhân viên",
    "Dán URL hoặc ID Google Sheet mẫu dùng để nhân bản cho nhân viên.",
    ui.ButtonSet.OK_CANCEL
  );

  if (response.getSelectedButton() !== ui.Button.OK) return;

  try {
    const templateId = provisioningExtractDriveId_(response.getResponseText());
    SpreadsheetApp.openById(templateId);
    PropertiesService.getScriptProperties().setProperty(
      EMPLOYEE_PROVISIONING_CONFIG_.TEMPLATE_ID_PROPERTY,
      templateId
    );
    ui.alert("Đã lưu file mẫu nhân viên.", "ID file mẫu: " + templateId, ui.ButtonSet.OK);
  } catch (error) {
    ui.alert("Không lưu được file mẫu", provisioningErrorMessage_(error), ui.ButtonSet.OK);
  }
}

function setEmployeeOutputFolderId() {
  const ui = SpreadsheetApp.getUi();
  const response = ui.prompt(
    "Cài thư mục lưu file nhân viên",
    "Dán URL hoặc ID folder Drive. Để trống rồi bấm OK để dùng cùng thư mục với file trung tâm.",
    ui.ButtonSet.OK_CANCEL
  );

  if (response.getSelectedButton() !== ui.Button.OK) return;

  const rawValue = provisioningText_(response.getResponseText());
  const props = PropertiesService.getScriptProperties();

  try {
    if (!rawValue) {
      props.deleteProperty(EMPLOYEE_PROVISIONING_CONFIG_.OUTPUT_FOLDER_ID_PROPERTY);
      ui.alert("Đã bỏ cài đặt thư mục riêng.", "File mới sẽ được tạo cùng thư mục với file trung tâm.", ui.ButtonSet.OK);
      return;
    }

    const folderId = provisioningExtractDriveId_(rawValue);
    const folder = DriveApp.getFolderById(folderId);
    props.setProperty(EMPLOYEE_PROVISIONING_CONFIG_.OUTPUT_FOLDER_ID_PROPERTY, folderId);
    ui.alert("Đã lưu thư mục tạo file.", "Thư mục: " + folder.getName(), ui.ButtonSet.OK);
  } catch (error) {
    ui.alert("Không lưu được thư mục", provisioningErrorMessage_(error), ui.ButtonSet.OK);
  }
}

function createEmployeeFilesForSelectedRows() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = requireEmployeeConfigSheet_();
  const activeSheet = ss.getActiveSheet();

  if (activeSheet.getName() !== EMPLOYEE_PROVISIONING_CONFIG_.CONFIG_SHEET_NAME) {
    SpreadsheetApp.getUi().alert("Hãy chọn dòng trong sheet Danh sach nhan vien rồi chạy lại.");
    return;
  }

  const activeRange = activeSheet.getActiveRange();
  if (!activeRange) {
    SpreadsheetApp.getUi().alert("Chưa chọn dòng nhân viên nào.");
    return;
  }

  const firstRow = Math.max(activeRange.getRow(), EMPLOYEE_PROVISIONING_CONFIG_.START_ROW);
  const lastRow = activeRange.getLastRow();
  if (lastRow < EMPLOYEE_PROVISIONING_CONFIG_.START_ROW) {
    SpreadsheetApp.getUi().alert("Vùng chọn chưa có dòng nhân viên hợp lệ.");
    return;
  }

  let rowNumbers = [];
  for (let row = firstRow; row <= lastRow; row++) {
    rowNumbers.push(row);
  }

  try {
    const summary = provisionEmployeeFilesForRows_(sheet, rowNumbers);
    showEmployeeProvisioningSummary_(summary);
  } catch (error) {
    SpreadsheetApp.getUi().alert("Không tạo được file nhân viên", provisioningErrorMessage_(error), SpreadsheetApp.getUi().ButtonSet.OK);
  }
}

function createEmployeeFilesForRowsMissingLink() {
  const sheet = requireEmployeeConfigSheet_();
  const lastRow = sheet.getLastRow();

  if (lastRow < EMPLOYEE_PROVISIONING_CONFIG_.START_ROW) {
    SpreadsheetApp.getUi().alert("Sheet Danh sach nhan vien chưa có dữ liệu nhân viên.");
    return;
  }

  const rowCount = lastRow - EMPLOYEE_PROVISIONING_CONFIG_.START_ROW + 1;
  const rows = sheet.getRange(EMPLOYEE_PROVISIONING_CONFIG_.START_ROW, 1, rowCount, 6).getValues();
  let rowNumbers = [];

  rows.forEach(function(row, index) {
    const employeeName = provisioningText_(row[EMPLOYEE_PROVISIONING_CONFIG_.COL_EMPLOYEE_NAME - 1]);
    const spreadsheetUrl = provisioningText_(row[EMPLOYEE_PROVISIONING_CONFIG_.COL_SPREADSHEET_URL - 1]);
    if (employeeName && !spreadsheetUrl) {
      rowNumbers.push(EMPLOYEE_PROVISIONING_CONFIG_.START_ROW + index);
    }
  });

  if (rowNumbers.length === 0) {
    SpreadsheetApp.getUi().alert("Không có dòng nào cần tạo file mới. Cột B đều đã có link hoặc dòng chưa có tên.");
    return;
  }

  try {
    const summary = provisionEmployeeFilesForRows_(sheet, rowNumbers);
    showEmployeeProvisioningSummary_(summary);
  } catch (error) {
    SpreadsheetApp.getUi().alert("Không tạo được file nhân viên", provisioningErrorMessage_(error), SpreadsheetApp.getUi().ButtonSet.OK);
  }
}

function provisionEmployeeFilesForRows_(sheet, rowNumbers) {
  ensureEmployeeProvisioningColumns_(sheet);

  const templateId = requireEmployeeProvisioningProperty_(
    EMPLOYEE_PROVISIONING_CONFIG_.TEMPLATE_ID_PROPERTY,
    "Chưa cài ID file mẫu nhân viên. Vào menu Công nợ > Cài ID file mẫu nhân viên."
  );
  const destinationFolder = getEmployeeDestinationFolder_();
  const summary = { created: 0, skipped: 0, warnings: 0, errors: 0, lines: [] };
  const processedRows = {};

  rowNumbers.forEach(function(rowNumber) {
    if (processedRows[rowNumber]) return;
    processedRows[rowNumber] = true;

    const values = sheet.getRange(rowNumber, 1, 1, 6).getValues()[0];
    const employeeName = provisioningText_(values[EMPLOYEE_PROVISIONING_CONFIG_.COL_EMPLOYEE_NAME - 1]);
    const existingUrl = provisioningText_(values[EMPLOYEE_PROVISIONING_CONFIG_.COL_SPREADSHEET_URL - 1]);
    const primaryEmails = values[EMPLOYEE_PROVISIONING_CONFIG_.COL_PRIMARY_EDITOR_EMAILS - 1];
    const reviewerEmails = values[EMPLOYEE_PROVISIONING_CONFIG_.COL_REVIEWER_EMAILS - 1];

    if (!employeeName) {
      summary.skipped++;
      summary.lines.push("Dòng " + rowNumber + ": bỏ qua vì chưa có tên nhân viên.");
      return;
    }
    if (existingUrl) {
      summary.skipped++;
      summary.lines.push("Dòng " + rowNumber + " (" + employeeName + "): bỏ qua vì cột B đã có link file.");
      return;
    }

    try {
      const fileName = EMPLOYEE_PROVISIONING_CONFIG_.FILE_NAME_PREFIX + employeeName;
      const copyFile = createEmployeeSpreadsheetCopy_(templateId, fileName, destinationFolder);
      const shareResult = shareEmployeeFile_(copyFile, primaryEmails, reviewerEmails);

      sheet.getRange(rowNumber, EMPLOYEE_PROVISIONING_CONFIG_.COL_SPREADSHEET_URL).setValue(copyFile.getUrl());
      summary.created++;

      if (shareResult.errors.length > 0) {
        summary.warnings++;
        summary.lines.push(
          "Dòng " + rowNumber + " (" + employeeName + "): đã tạo file nhưng có lỗi share: " + shareResult.errors.join("; ")
        );
      } else if (shareResult.editors.length > 0 || shareResult.viewers.length > 0) {
        summary.lines.push(
          "Dòng " + rowNumber + " (" + employeeName + "): đã tạo file; editor: "
          + (shareResult.editors.join(", ") || "không có")
          + "; viewer: " + (shareResult.viewers.join(", ") || "không có") + "."
        );
      } else {
        summary.warnings++;
        summary.lines.push(
          "Dòng " + rowNumber + " (" + employeeName + "): đã tạo file nhưng chưa share vì cột E/F trống."
        );
      }
    } catch (error) {
      summary.errors++;
      summary.lines.push("Dòng " + rowNumber + " (" + employeeName + "): lỗi " + provisioningErrorMessage_(error));
    }
  });

  return summary;
}

function syncEmployeeSharingForSelectedRows() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = requireEmployeeConfigSheet_();
  const activeSheet = ss.getActiveSheet();

  if (activeSheet.getName() !== EMPLOYEE_PROVISIONING_CONFIG_.CONFIG_SHEET_NAME) {
    SpreadsheetApp.getUi().alert("Hãy chọn dòng trong sheet Danh sach nhan vien rồi chạy lại.");
    return;
  }

  const activeRange = activeSheet.getActiveRange();
  if (!activeRange) {
    SpreadsheetApp.getUi().alert("Chưa chọn dòng nhân viên nào.");
    return;
  }

  const firstRow = Math.max(activeRange.getRow(), EMPLOYEE_PROVISIONING_CONFIG_.START_ROW);
  const lastRow = activeRange.getLastRow();
  if (lastRow < EMPLOYEE_PROVISIONING_CONFIG_.START_ROW) {
    SpreadsheetApp.getUi().alert("Vùng chọn chưa có dòng nhân viên hợp lệ.");
    return;
  }

  let rowNumbers = [];
  for (let row = firstRow; row <= lastRow; row++) {
    rowNumbers.push(row);
  }

  const summary = syncEmployeeSharingForRows_(sheet, rowNumbers);
  showEmployeeSharingSummary_(summary);
}

function syncEmployeeSharingForRows_(sheet, rowNumbers) {
  ensureEmployeeProvisioningColumns_(sheet);

  const summary = { updated: 0, skipped: 0, warnings: 0, errors: 0, lines: [] };
  const processedRows = {};

  rowNumbers.forEach(function(rowNumber) {
    if (processedRows[rowNumber]) return;
    processedRows[rowNumber] = true;

    const values = sheet.getRange(rowNumber, 1, 1, 6).getValues()[0];
    const employeeName = provisioningText_(values[EMPLOYEE_PROVISIONING_CONFIG_.COL_EMPLOYEE_NAME - 1]);
    const rawSpreadsheetId = provisioningText_(values[EMPLOYEE_PROVISIONING_CONFIG_.COL_SPREADSHEET_URL - 1]);
    const primaryEmails = values[EMPLOYEE_PROVISIONING_CONFIG_.COL_PRIMARY_EDITOR_EMAILS - 1];
    const reviewerEmails = values[EMPLOYEE_PROVISIONING_CONFIG_.COL_REVIEWER_EMAILS - 1];

    if (!employeeName || !rawSpreadsheetId) {
      summary.skipped++;
      summary.lines.push("Dòng " + rowNumber + ": bỏ qua vì thiếu tên nhân viên hoặc link file.");
      return;
    }

    try {
      const spreadsheetId = provisioningExtractDriveId_(rawSpreadsheetId);
      const file = DriveApp.getFileById(spreadsheetId);
      const shareResult = shareEmployeeFile_(file, primaryEmails, reviewerEmails);
      summary.updated++;

      if (shareResult.errors.length > 0) {
        summary.warnings++;
        summary.lines.push(
          "Dòng " + rowNumber + " (" + employeeName + "): đã cập nhật nhưng có lỗi: " + shareResult.errors.join("; ")
        );
      } else {
        summary.lines.push(
          "Dòng " + rowNumber + " (" + employeeName + "): editor: "
          + (shareResult.editors.join(", ") || "không có")
          + "; viewer: " + (shareResult.viewers.join(", ") || "không có") + "."
        );
      }
    } catch (error) {
      summary.errors++;
      summary.lines.push("Dòng " + rowNumber + " (" + employeeName + "): lỗi " + provisioningErrorMessage_(error));
    }
  });

  return summary;
}

function createEmployeeSpreadsheetCopy_(templateId, fileName, destinationFolder) {
  const templateSpreadsheet = SpreadsheetApp.openById(templateId);
  const copySpreadsheet = templateSpreadsheet.copy(fileName);
  const copyFile = DriveApp.getFileById(copySpreadsheet.getId());

  if (destinationFolder) {
    copyFile.moveTo(destinationFolder);
  }

  return copyFile;
}

function shareEmployeeFile_(file, primaryEmails, reviewerEmails) {
  const editors = uniqueProvisioningValues_(parseProvisioningEmails_(primaryEmails));
  const viewerCandidates = uniqueProvisioningValues_(parseProvisioningEmails_(reviewerEmails));
  const editorLookup = {};
  editors.forEach(function(email) {
    editorLookup[String(email).toLowerCase()] = true;
  });
  const viewers = viewerCandidates.filter(function(email) {
    return !editorLookup[String(email).toLowerCase()];
  });
  let errors = [];

  editors.forEach(function(email) {
    try {
      file.addEditor(email);
    } catch (error) {
      errors.push(email + " (" + provisioningErrorMessage_(error) + ")");
    }
  });

  viewers.forEach(function(email) {
    try {
      try {
        file.removeEditor(email);
      } catch (removeError) {
        // Không phải editor sẵn thì bỏ qua, addViewer phía dưới vẫn là trạng thái đúng.
      }
      file.addViewer(email);
    } catch (error) {
      errors.push(email + " (" + provisioningErrorMessage_(error) + ")");
    }
  });

  return { editors: editors, viewers: viewers, errors: errors };
}

function ensureEmployeeProvisioningColumns_(sheet) {
  const headers = [["Email người điền chính", "Email người review"]];
  sheet.getRange(3, EMPLOYEE_PROVISIONING_CONFIG_.COL_PRIMARY_EDITOR_EMAILS, 1, 2).setValues(headers).setFontWeight("bold");
  sheet.autoResizeColumns(EMPLOYEE_PROVISIONING_CONFIG_.COL_PRIMARY_EDITOR_EMAILS, 2);
}

function requireEmployeeConfigSheet_() {
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(EMPLOYEE_PROVISIONING_CONFIG_.CONFIG_SHEET_NAME);
  if (!sheet) {
    throw new Error("Không tìm thấy sheet cấu hình: " + EMPLOYEE_PROVISIONING_CONFIG_.CONFIG_SHEET_NAME);
  }
  return sheet;
}

function requireEmployeeProvisioningProperty_(propertyName, missingMessage) {
  const value = PropertiesService.getScriptProperties().getProperty(propertyName);
  if (!value) {
    throw new Error(missingMessage);
  }
  return value;
}

function getEmployeeDestinationFolder_() {
  const folderId = PropertiesService.getScriptProperties().getProperty(
    EMPLOYEE_PROVISIONING_CONFIG_.OUTPUT_FOLDER_ID_PROPERTY
  );
  if (folderId) {
    return DriveApp.getFolderById(folderId);
  }

  const masterFile = DriveApp.getFileById(SpreadsheetApp.getActiveSpreadsheet().getId());
  const parents = masterFile.getParents();
  return parents.hasNext() ? parents.next() : null;
}

function parseProvisioningEmails_(value) {
  const text = provisioningText_(value);
  if (!text) return [];

  return text
    .split(/[,\n;]+/)
    .map(function(item) { return provisioningText_(item); })
    .filter(function(item) { return item && item.indexOf("@") > 0; });
}

function uniqueProvisioningValues_(values) {
  let seen = {};
  let result = [];

  values.forEach(function(value) {
    const key = String(value).toLowerCase();
    if (!seen[key]) {
      seen[key] = true;
      result.push(value);
    }
  });

  return result;
}

function provisioningExtractDriveId_(rawValue) {
  const value = provisioningText_(rawValue);
  const fileMatch = value.match(/\/d\/([a-zA-Z0-9_-]+)/);
  if (fileMatch && fileMatch[1]) return fileMatch[1];

  const folderMatch = value.match(/\/folders\/([a-zA-Z0-9_-]+)/);
  if (folderMatch && folderMatch[1]) return folderMatch[1];

  const idMatch = value.match(/[a-zA-Z0-9_-]{20,}/);
  if (idMatch && idMatch[0]) return idMatch[0];

  throw new Error("URL/ID Drive không hợp lệ.");
}

function showEmployeeProvisioningSummary_(summary) {
  const ui = SpreadsheetApp.getUi();
  const maxLines = 20;
  let lines = [
    "Đã tạo: " + summary.created,
    "Bỏ qua: " + summary.skipped,
    "Cảnh báo: " + summary.warnings,
    "Lỗi: " + summary.errors,
    ""
  ].concat(summary.lines.slice(0, maxLines));

  if (summary.lines.length > maxLines) {
    lines.push("... còn " + (summary.lines.length - maxLines) + " dòng khác, xem Execution log nếu cần.");
  }

  ui.alert("Kết quả nhân bản file nhân viên", lines.join("\n"), ui.ButtonSet.OK);
}

function showEmployeeSharingSummary_(summary) {
  const ui = SpreadsheetApp.getUi();
  const maxLines = 20;
  let lines = [
    "Đã cập nhật: " + summary.updated,
    "Bỏ qua: " + summary.skipped,
    "Cảnh báo: " + summary.warnings,
    "Lỗi: " + summary.errors,
    ""
  ].concat(summary.lines.slice(0, maxLines));

  if (summary.lines.length > maxLines) {
    lines.push("... còn " + (summary.lines.length - maxLines) + " dòng khác, xem Execution log nếu cần.");
  }

  ui.alert("Kết quả cập nhật quyền chia sẻ", lines.join("\n"), ui.ButtonSet.OK);
}

function provisioningText_(value) {
  return value === null || value === undefined ? "" : String(value).trim();
}

function provisioningErrorMessage_(error) {
  const message = error && error.message ? error.message : String(error || "Không rõ lỗi");
  return message.length > 900 ? message.substring(0, 900) : message;
}
