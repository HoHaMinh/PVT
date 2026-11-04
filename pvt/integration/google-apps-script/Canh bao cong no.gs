function masterCheckAllFilesAndSendEmail() {
  const CONFIG_SHEET_NAME = "Danh sach nhan vien";
  
  const ssMaster = SpreadsheetApp.getActiveSpreadsheet();
  const configSheet = ssMaster.getSheetByName(CONFIG_SHEET_NAME);
  
  if (!configSheet) {
    Logger.log("Không tìm thấy sheet cấu hình: " + CONFIG_SHEET_NAME);
    return;
  }
  
  let daysOffset = configSheet.getRange("B1").getValue();
  if (isNaN(daysOffset) || daysOffset === "") {
    daysOffset = 7; 
  }
  Logger.log(`Hệ thống đang quét cảnh báo theo mốc: t + ${daysOffset} ngày.`);
  
  const today = new Date();
  today.setHours(0,0,0,0);
  
  if (today.getDay() === 0) { 
    Logger.log("Hôm nay là Chủ Nhật. Hệ thống nghỉ không gửi mail.");
    return; 
  }
  
  if (isHoliday(today)) {
    Logger.log("Hôm nay là ngày nghỉ Lễ/Tết. Hệ thống dừng gửi mail.");
    return;
  }
  
  const lastRowConfig = configSheet.getLastRow();
  if (lastRowConfig < 4) {
    Logger.log("Chưa có dữ liệu cấu hình nhân viên từ hàng 4.");
    return;
  }
  const configData = configSheet.getRange(4, 1, lastRowConfig - 3, 3).getValues();
  
  const targetDate = new Date();
  targetDate.setDate(today.getDate() + daysOffset);
  targetDate.setHours(23,59,59,999);
  
  for (let k = 0; k < configData.length; k++) {
    let employeeName = configData[k][0];
    let rawSpreadsheetId = String(configData[k][1]).trim(); 
    let emailReceivers = configData[k][2];
    
    if (!rawSpreadsheetId || !emailReceivers) {
      Logger.log(`Bỏ qua nhân viên ${employeeName} do thiếu ID File hoặc Email.`);
      continue;
    }
    
    let spreadsheetId = rawSpreadsheetId;
    if (rawSpreadsheetId.includes("docs.google.com/spreadsheets")) {
      let matches = rawSpreadsheetId.match(/\/d\/([^/]+)/);
      if (matches && matches[1]) {
        spreadsheetId = matches[1]; 
      }
    }
    
    try {
      let empSpreadsheet = SpreadsheetApp.openById(spreadsheetId);
      let allSheets = empSpreadsheet.getSheets();
      
      let validReportSheets = [];
      let isDuplicateFound = false;
      let duplicateInfo = "";
      
      // Bước A: Lọc toàn bộ các sheet báo cáo hợp lệ và kiểm tra trùng đợt/tháng trước
      allSheets.forEach(function(sheet) {
        let titleA1 = String(sheet.getRange("A1").getValue()).trim();
        if (titleA1 === "BIÊN BẢN BÁO CÁO HÓA ĐƠN CÔNG NỢ") {
          let dotValue = String(sheet.getRange("D2").getValue()).trim();
          let thangValue = String(sheet.getRange("F2").getValue()).trim();
          
          validReportSheets.push({
            sheetObj: sheet,
            dot: dotValue,
            thang: thangValue
          });
        }
      });
      
      // Kiểm tra trùng lặp chéo giữa các sheet báo cáo tìm được
      for (let i = 0; i < validReportSheets.length; i++) {
        for (let j = i + 1; j < validReportSheets.length; j++) {
          if (validReportSheets[i].dot === validReportSheets[j].dot && validReportSheets[i].thang === validReportSheets[j].thang) {
            isDuplicateFound = true;
            duplicateInfo = `ĐỢT ${validReportSheets[i].dot.toUpperCase()} THÁNG ${validReportSheets[i].thang.toUpperCase()}`;
            break;
          }
        }
        if (isDuplicateFound) break;
      }
      
      // NẾU TRÙNG NHAU: Gửi mail báo lỗi đặc biệt và bỏ qua quét công nợ nhân viên này
      if (isDuplicateFound) {
        let errorBody = `<h3 style="color: red;">CẢNH BÁO LỖI HỆ THỐNG KIỂM TRA</h3>` +
                        `<p style="font-size: 14px; font-weight: bold; color: #333;">` +
                        `HIỆN CÓ SHEET TRÙNG NHAU ${duplicateInfo} NÊN HỆ THỐNG KHÔNG CẢNH BÁO CÔNG NỢ ĐƯỢC` +
                        `</p>` +
                        `<p>Vui lòng kiểm tra lại file dữ liệu của <b>${employeeName}</b> để sửa đổi đợt/tháng khai báo trùng lặp.</p>`;
                        
        MailApp.sendEmail({
          to: emailReceivers,
          subject: `[LỖI KHAI BÁO CÔNG NỢ] - File ${employeeName} không thể thực hiện cảnh báo do trùng đợt`,
          htmlBody: errorBody
        });
        
        Logger.log(`File của ${employeeName} bị trùng cấu hình [${duplicateInfo}]. Đã gửi mail thông báo lỗi.`);
        continue; // Chuyển sang nhân viên tiếp theo
      }
      
      // Bước B: Nếu không trùng thì tiến hành tìm sheet mới nhất để quét công nợ như bình thường
      let newestReport = null;
      let finalDot = "";
      let finalThang = "";
      
      const dotWeights = {
        "Đợt 1+2": 1,
        "Đợt 3+4": 2,
        "Đợt 5+6": 3,
        "Đợt 7": 4
      };
      
      validReportSheets.forEach(function(item) {
        if (!newestReport || isDebtAlertReportLater_(item, newestReport, dotWeights)) {
          newestReport = item;
          finalDot = item.dot;
          finalThang = item.thang;
        }
      });
      let newestSheet = newestReport ? newestReport.sheetObj : null;
      
      if (!newestSheet) {
        Logger.log(`Không tìm thấy sheet công nợ hợp lệ nào trong file của: ${employeeName}`);
        continue;
      }
      
      let lastRowData = newestSheet.getLastRow();
      if (lastRowData < 3) {
        Logger.log(`Sheet mới nhất của ${employeeName} không có dữ liệu hàng.`);
        continue;
      }
      
      let dataRange = newestSheet.getRange(2, 1, lastRowData - 1, 13);
      let data = dataRange.getValues();
      
      let listAlert = [];
      let currentSection = "";
      
      for (let i = 0; i < data.length; i++) {
        let row = data[i];
        let colA = String(row[0]).trim();
        
        if (colA.startsWith("II.") || colA.startsWith("III.")) {
          currentSection = colA;
          continue;
        }
        if (colA.match(/^[I|V|X]+\./) && !colA.startsWith("II.") && !colA.startsWith("III.")) {
          currentSection = "";
          continue;
        }
        
        if (currentSection.startsWith("II.") || currentSection.startsWith("III.")) {
          let dueDateValue = row[7];
          
          if (dueDateValue instanceof Date) {
            let dueDate = new Date(dueDateValue);
            dueDate.setHours(0,0,0,0);
            
            if (dueDate <= targetDate) {
              let msPerDay = 24 * 60 * 60 * 1000;
              let tPlusValue = Math.round((today - dueDate) / msPerDay);
              
              listAlert.push({
                section:    currentSection.split(".")[0] + ".",
                stt:        row[0],
                ncc:        row[1],
                invoiceNo:  row[2],
                amount:     row[4],
                dueDateStr: Utilities.formatDate(dueDate, Session.getScriptTimeZone(), "dd/MM/yyyy"),
                tPlusValue: tPlusValue
              });
            }
          }
        }
      }
      
      if (listAlert.length > 0) {
        listAlert.sort(function(a, b) { return b.tPlusValue - a.tPlusValue; });

        let emailBody = `<h3>Thông báo từ hệ thống quản lý công nợ (File: ${employeeName})</h3>`;
        emailBody += `<p>Chào anh/chị, sáng nay ngày ${Utilities.formatDate(today, Session.getScriptTimeZone(), "dd/MM/yyyy")}, hệ thống phát hiện file của <b>${employeeName}</b> ` +
                     `<span style="color: red; font-weight: bold;">${String(finalDot).toUpperCase()}</span> ` +
                     `<span style="color: red; font-weight: bold;">THÁNG ${String(finalThang).replace(/[^0-9]/g, "")}</span> ` +
                     `có <b>${listAlert.length}</b> bộ hồ sơ thanh toán cần chú ý (trễ hạn hoặc sắp đến hạn t+${daysOffset}):</p>`;
        
        emailBody += `<table border="1" cellpadding="5" style="border-collapse: collapse; font-family: Arial, sans-serif;">
                        <tr style="background-color: #f2f2f2;">
                          <th>Mục</th>
                          <th>TT</th>
                          <th>Nhà Cung Cấp</th>
                          <th>Số Hóa Đơn</th>
                          <th>Trị Giá (VND/USD)</th>
                          <th>Ngày Đến Hạn</th>
                          <th>T+</th>
                        </tr>`;
                        
        listAlert.forEach(function(item) {
          let tPlusLabel, tPlusColor;
          if (item.tPlusValue > 0) {
            tPlusLabel = `+${item.tPlusValue} (Trễ ${item.tPlusValue} ngày)`;
            tPlusColor = "#cc0000";
          } else if (item.tPlusValue === 0) {
            tPlusLabel = "Hôm nay";
            tPlusColor = "#e65c00";
          } else {
            tPlusLabel = `${item.tPlusValue} (Còn ${Math.abs(item.tPlusValue)} ngày)`;
            tPlusColor = "#276221";
          }

          emailBody += `<tr>
                          <td>${item.section}</td>
                          <td align="center">${item.stt}</td>
                          <td>${item.ncc}</td>
                          <td>${item.invoiceNo}</td>
                          <td>${typeof item.amount === 'number' ? item.amount.toLocaleString() : item.amount}</td>
                          <td style="color: red; font-weight: bold;">${item.dueDateStr}</td>
                          <td style="color: ${tPlusColor}; font-weight: bold; text-align: center;">${tPlusLabel}</td>
                        </tr>`;
        });
        
        emailBody += `</table><br><p>Vui lòng kiểm tra lại file trên Drive để thực hiện thanh toán đúng hạn.</p>`;
        
        MailApp.sendEmail({
          to: emailReceivers,
          subject: `[CẢNH BÁO CÔNG NỢ TỰ ĐỘNG - NO REPLY] - File ${employeeName} (${finalDot} - Tháng ${finalThang}) sắp đến hạn t+${daysOffset}`,
          htmlBody: emailBody
        });
        Logger.log(`Đã gửi mail thành công cho file của ${employeeName} (Dữ liệu lấy từ sheet: ${newestSheet.getName()}).`);
      } else {
        Logger.log(`File của ${employeeName} không có bộ công nợ nào cần cảnh báo.`);
      }
      
    } catch (err) {
      Logger.log(`Lỗi khi xử lý file của nhân viên ${employeeName}: ` + err.toString());
    }
  }
}

function isDebtAlertReportLater_(candidate, current, dotWeights) {
  const candidateMonth = parseInt(String(candidate.thang).replace(/[^0-9]/g, ""), 10) || 0;
  const currentMonth = parseInt(String(current.thang).replace(/[^0-9]/g, ""), 10) || 0;
  if (candidateMonth === currentMonth) {
    return (dotWeights[candidate.dot] || 0) > (dotWeights[current.dot] || 0);
  }
  if (candidateMonth === 1 && currentMonth === 12) return true;
  if (candidateMonth === 12 && currentMonth === 1) return false;
  return candidateMonth > currentMonth;
}

function isHoliday(checkDate) {
  try {
    const checkDateStr = Utilities.formatDate(checkDate, Session.getScriptTimeZone(), "dd/MM/yyyy");
    const holidaySheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName("Lich nghi le");
    if (!holidaySheet) return false;
    
    const lastRow = holidaySheet.getLastRow();
    if (lastRow < 1) return false;
    
    const holidayDates = holidaySheet.getRange(1, 1, lastRow, 1).getValues();
    
    for (let i = 0; i < holidayDates.length; i++) {
      let hDate = holidayDates[i][0];
      if (hDate instanceof Date) {
        let hDateStr = Utilities.formatDate(hDate, Session.getScriptTimeZone(), "dd/MM/yyyy");
        if (checkDateStr === hDateStr) {
          return true;
        }
      }
    }
  } catch (e) {
    Logger.log("Lỗi kiểm tra lịch lễ: " + e.toString());
  }
  return false;
}
