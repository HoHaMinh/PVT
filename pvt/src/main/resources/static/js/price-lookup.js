(function () {
    "use strict";

    function showLoading(message) {
        var overlay = document.getElementById("lookupLoading");
        var label = document.getElementById("lookupLoadingLabel");
        if (label && message) label.textContent = message;
        if (overlay) overlay.classList.add("is-visible");
    }

    function hasBatchSelection(form) {
        return Boolean(form.querySelector(".candidate-checkbox:checked, .no-suitable-checkbox:checked")
                || form.querySelector("[data-no-candidates='true']"));
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("form[data-loading-message]").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                if (!form.checkValidity()) return;
                if (form.hasAttribute("data-batch-confirmation") && !hasBatchSelection(form)) {
                    event.preventDefault();
                    window.alert("Hãy chọn tên vật tư hoặc chọn không có tên đề xuất phù hợp.");
                    return;
                }
                showLoading(form.getAttribute("data-loading-message"));
                form.querySelectorAll("button[type='submit']").forEach(function (button) {
                    button.disabled = true;
                });
            });
        });

        document.querySelectorAll(".lookup-selection-group[data-max-selections]").forEach(function (group) {
            var maximum = Number(group.getAttribute("data-max-selections")) || 12;
            var candidates = group.querySelectorAll(".candidate-checkbox");
            var noSuitable = group.querySelector(".no-suitable-checkbox");

            function updateCounter() {
                var counter = group.querySelector("[data-selected-count]");
                if (counter) counter.textContent = group.querySelectorAll(".candidate-checkbox:checked").length;
            }

            group.addEventListener("change", function (event) {
                if (!event.target.matches("input[type='checkbox']")) return;
                if (event.target.classList.contains("candidate-checkbox") && event.target.checked && noSuitable) {
                    noSuitable.checked = false;
                }
                if (event.target.classList.contains("no-suitable-checkbox") && event.target.checked) {
                    candidates.forEach(function (candidate) { candidate.checked = false; });
                }
                var checked = group.querySelectorAll(".candidate-checkbox:checked");
                if (checked.length > maximum) {
                    event.target.checked = false;
                    window.alert("Mỗi hạng mục chỉ được chọn tối đa " + maximum + " tên vật tư.");
                }
                updateCounter();
            });
            updateCounter();
        });

        document.querySelectorAll(".copy-button[data-copy]").forEach(function (button) {
            button.addEventListener("click", function () {
                navigator.clipboard.writeText(button.getAttribute("data-copy") || "").then(function () {
                    button.classList.add("is-copied");
                    window.setTimeout(function () { button.classList.remove("is-copied"); }, 900);
                });
            });
        });
    });
}());
