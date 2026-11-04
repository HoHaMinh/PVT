package com.hoaphat.pvt.controller;

import com.hoaphat.pvt.model.dto.UserDisplayDTO;
import com.hoaphat.pvt.model.dto.price.PriceLookupBatchConfirmationForm;
import com.hoaphat.pvt.model.dto.price.PriceLookupBatchConfirmationResult;
import com.hoaphat.pvt.model.price.PriceLookupItem;
import com.hoaphat.pvt.service.account.AccountService;
import com.hoaphat.pvt.service.price.PriceLookupService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collections;
import java.util.List;

@Controller
public class PriceLookupController {
    private final PriceLookupService priceLookupService;
    private final AccountService accountService;

    public PriceLookupController(PriceLookupService priceLookupService,
                                 AccountService accountService) {
        this.priceLookupService = priceLookupService;
        this.accountService = accountService;
    }

    @InitBinder("confirmationForm")
    public void configureConfirmationBinding(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(500);
    }

    @GetMapping("/home/employee/price-lookups")
    public String showPending(@RequestParam(value = "selectedValue", required = false) String selectedValue,
                              Model model, Principal principal, HttpServletRequest request) {
        UserContext context = userContext(principal, request, selectedValue);
        model.addAttribute("pendingItems", priceLookupService.getPending(
                context.accountName, context.manager, context.selectedAccount));
        addCommonModel(model, request, context);
        return "priceLookupPending";
    }

    @PostMapping("/home/manager/price-lookups")
    public String createBatch(@RequestParam("requestLines") String requestLines,
                              Principal principal, RedirectAttributes redirectAttributes) {
        try {
            int count = priceLookupService.createBatch(requestLines, principal.getName());
            redirectAttributes.addFlashAttribute("mess",
                    "Đã tạo " + count + " hạng mục và chuyển đến đúng nhân viên để xác nhận.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", safeMessage(e));
            redirectAttributes.addFlashAttribute("requestLines", requestLines);
        }
        return "redirect:/home/employee/price-lookups";
    }

    @PostMapping("/home/employee/price-lookups/{itemId}/confirm")
    public String confirm(@PathVariable Integer itemId,
                          @RequestParam(value = "selectedCandidateIds", required = false) List<Integer> selectedCandidateIds,
                          @RequestParam(value = "selectedValue", required = false) String selectedValue,
                          Principal principal, HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            String error = priceLookupService.confirm(itemId,
                    selectedCandidateIds == null ? Collections.emptyList() : selectedCandidateIds,
                    principal.getName(), request.isUserInRole("ROLE_MANAGER"));
            if (error.isEmpty()) {
                redirectAttributes.addFlashAttribute("mess", "Đã xác nhận và lưu kết quả tra giá.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Chưa thể tra giá: " + error);
            }
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", safeMessage(e));
        }
        if (selectedValue != null && !selectedValue.trim().isEmpty()) {
            redirectAttributes.addAttribute("selectedValue", selectedValue.trim());
        }
        return "redirect:/home/employee/price-lookups";
    }

    @PostMapping("/home/employee/price-lookups/confirm-batch")
    public String confirmBatch(
            @ModelAttribute("confirmationForm") PriceLookupBatchConfirmationForm confirmationForm,
            @RequestParam(value = "selectedValue", required = false) String selectedValue,
            Principal principal, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            PriceLookupBatchConfirmationResult result = priceLookupService.confirmBatch(
                    confirmationForm.getItems(), principal.getName(),
                    request.isUserInRole("ROLE_MANAGER"));
            if (result.getCompletedCount() > 0) {
                redirectAttributes.addFlashAttribute("mess", "Đã xác nhận "
                        + result.getCompletedCount() + " hạng mục và lưu kết quả tra giá.");
            }
            if (result.getFailedCount() > 0) {
                redirectAttributes.addFlashAttribute("error", "Có " + result.getFailedCount()
                        + " hạng mục chưa thể tra giá. Hệ thống giữ lại để thử lại sau.");
            } else if (result.getProcessedCount() == 0) {
                redirectAttributes.addFlashAttribute("error",
                        "Hãy chọn tên vật tư hoặc chọn không có tên đề xuất phù hợp.");
            }
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", safeMessage(e));
        }
        addSelectedValue(selectedValue, redirectAttributes);
        return "redirect:/home/employee/price-lookups";
    }

    @PostMapping("/home/employee/price-lookups/{itemId}/dismiss")
    public String dismissNoMatch(@PathVariable Integer itemId,
                                 @RequestParam(value = "selectedValue", required = false) String selectedValue,
                                 Principal principal, HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            priceLookupService.dismissNoMatch(itemId, principal.getName(),
                    request.isUserInRole("ROLE_MANAGER"));
            redirectAttributes.addFlashAttribute("mess", "Đã xác nhận hạng mục không có tên vật tư phù hợp.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", safeMessage(e));
        }
        addSelectedValue(selectedValue, redirectAttributes);
        return "redirect:/home/employee/price-lookups";
    }

    @GetMapping("/home/employee/price-lookup-results")
    public String showResults(@RequestParam(value = "selectedValue", required = false) String selectedValue,
                              @RequestParam(value = "search", required = false) String search,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              Model model, Principal principal, HttpServletRequest request) {
        UserContext context = userContext(principal, request, selectedValue);
        String normalizedSearch = search == null ? "" : search.trim();
        Page<PriceLookupItem> resultPage = priceLookupService.searchResults(
                context.accountName, context.manager, context.selectedAccount, normalizedSearch, page);
        model.addAttribute("resultPage", resultPage);
        model.addAttribute("search", normalizedSearch);
        model.addAttribute("trashMode", false);
        addCommonModel(model, request, context);
        return "priceLookupResults";
    }

    @GetMapping("/home/manager/price-lookup-results/trash")
    public String showDeletedResults(
            @RequestParam(value = "selectedValue", required = false) String selectedValue,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model, Principal principal, HttpServletRequest request) {
        UserContext context = userContext(principal, request, selectedValue);
        String normalizedSearch = search == null ? "" : search.trim();
        Page<PriceLookupItem> resultPage = priceLookupService.searchDeletedResults(
                context.manager, context.selectedAccount, normalizedSearch, page);
        model.addAttribute("resultPage", resultPage);
        model.addAttribute("search", normalizedSearch);
        model.addAttribute("trashMode", true);
        addCommonModel(model, request, context);
        return "priceLookupResults";
    }

    @PostMapping("/home/employee/price-lookup-results/{itemId}/trash")
    public String moveResultToTrash(
            @PathVariable Integer itemId,
            @RequestParam(value = "selectedValue", required = false) String selectedValue,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Principal principal, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            priceLookupService.moveResultToTrash(itemId, principal.getName(),
                    request.isUserInRole("ROLE_MANAGER"));
            redirectAttributes.addFlashAttribute("mess", "Đã chuyển kết quả tra giá vào thư mục đã xóa.");
        } catch (SecurityException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", safeMessage(e));
        }
        addResultViewState(selectedValue, search, page, redirectAttributes);
        return "redirect:/home/employee/price-lookup-results";
    }

    @PostMapping("/home/manager/price-lookup-results/trash/{itemId}/restore")
    public String restoreDeletedResult(
            @PathVariable Integer itemId,
            @RequestParam(value = "selectedValue", required = false) String selectedValue,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            priceLookupService.restoreDeletedResult(itemId,
                    request.isUserInRole("ROLE_MANAGER"));
            redirectAttributes.addFlashAttribute("mess", "Đã khôi phục kết quả tra giá.");
        } catch (SecurityException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", safeMessage(e));
        }
        addResultViewState(selectedValue, search, page, redirectAttributes);
        return "redirect:/home/manager/price-lookup-results/trash";
    }

    @PostMapping("/home/manager/price-lookup-results/trash/empty")
    public String emptyDeletedResults(HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        try {
            int deletedCount = priceLookupService.emptyDeletedResults(
                    request.isUserInRole("ROLE_MANAGER"));
            redirectAttributes.addFlashAttribute("mess", deletedCount == 0
                    ? "Thư mục kết quả tra giá đã xóa đang trống."
                    : "Đã xóa hẳn " + deletedCount + " kết quả tra giá.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", safeMessage(e));
        }
        return "redirect:/home/manager/price-lookup-results/trash";
    }

    private UserContext userContext(Principal principal, HttpServletRequest request, String selectedValue) {
        String accountName = principal == null ? "" : principal.getName();
        boolean manager = request.isUserInRole("ROLE_MANAGER");
        String selectedAccount = manager
                ? (selectedValue == null ? "" : selectedValue.trim()) : accountName;
        return new UserContext(accountName, manager, selectedAccount);
    }

    private void addCommonModel(Model model, HttpServletRequest request, UserContext context) {
        List<UserDisplayDTO> userTabs = accountService.getUserDisplayList();
        model.addAttribute("userTabs", userTabs);
        model.addAttribute("isManager", context.manager);
        model.addAttribute("accountName", context.accountName);
        model.addAttribute("selectedValue", context.selectedAccount);
        model.addAttribute("username", request.getSession().getAttribute("username"));
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty() ? "Không thể xử lý yêu cầu tra giá." : message;
    }

    private void addSelectedValue(String selectedValue, RedirectAttributes redirectAttributes) {
        if (selectedValue != null && !selectedValue.trim().isEmpty()) {
            redirectAttributes.addAttribute("selectedValue", selectedValue.trim());
        }
    }

    private void addResultViewState(String selectedValue, String search, int page,
                                    RedirectAttributes redirectAttributes) {
        addSelectedValue(selectedValue, redirectAttributes);
        if (search != null && !search.trim().isEmpty()) {
            redirectAttributes.addAttribute("search", search.trim());
        }
        if (page > 0) {
            redirectAttributes.addAttribute("page", page);
        }
    }

    private static class UserContext {
        private final String accountName;
        private final boolean manager;
        private final String selectedAccount;

        private UserContext(String accountName, boolean manager, String selectedAccount) {
            this.accountName = accountName;
            this.manager = manager;
            this.selectedAccount = selectedAccount;
        }
    }
}
