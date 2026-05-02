/**
 * Top-level section pages (omni_section_header): mark `li` for styles, replace
 * the section anchor with a span so the label is not a link; expander keeps
 * default JTD absolute layout (do not use CSS grid on these rows — it breaks
 * nested gRPC / multi-level chevrons).
 */
(function () {
  var el = document.getElementById("omni-nav-section-urls");
  if (!el) return;

  var urls;
  try {
    urls = JSON.parse(el.textContent);
  } catch (e) {
    return;
  }

  if (!urls || !urls.length) return;

  function normalizeHref(href) {
    if (!href) return "";
    try {
      var u = new URL(href, window.location.origin);
      return u.pathname.replace(/\/$/, "") || "/";
    } catch (e) {
      return href.split("?")[0].replace(/\/$/, "") || "/";
    }
  }

  var normalized = new Set();
  urls.forEach(function (u) {
    normalized.add(normalizeHref(u));
  });

  var nav = document.getElementById("site-nav");
  if (!nav) return;

  var topLis = nav.querySelectorAll(":scope > ul.nav-list > li.nav-list-item");
  topLis.forEach(function (li) {
    var link = li.querySelector(":scope > a.nav-list-link");
    if (!link) return;
    var href = link.getAttribute("href");
    if (!href) return;
    if (!normalized.has(normalizeHref(href))) return;

    li.classList.add("omni-nav-section-item");

    var span = document.createElement("span");
    span.className = link.className + " omni-nav-section-title";
    span.textContent = link.textContent;
    link.replaceWith(span);
  });
})();

/**
 * After JTD initNav + activateNav: open only root sections (top-level
 * #site-nav > ul > li); nested rows (e.g. gRPC) stay closed until expanded or
 * opened by activateNav for the current page. Section title span matches chevron.
 */
(function () {
  function expandRootNavSubmenus() {
    var nav = document.getElementById("site-nav");
    if (!nav) return;
    nav
      .querySelectorAll(":scope > ul.nav-list > li.nav-list-item")
      .forEach(function (li) {
        if (!li.querySelector(":scope > ul.nav-list")) return;
        li.classList.add("active");
        var btn = li.querySelector(":scope > .nav-list-expander");
        if (btn) btn.setAttribute("aria-pressed", "true");
      });
  }

  function bindSectionHeaderToggles() {
    document
      .querySelectorAll("span.nav-list-link.omni-nav-section-title")
      .forEach(function (span) {
        span.addEventListener("click", function (e) {
          e.preventDefault();
          e.stopPropagation();
          var li = span.closest("li.nav-list-item");
          if (!li) return;
          var btn = li.querySelector(":scope > .nav-list-expander");
          if (btn) btn.click();
        });
      });
  }

  var run = function () {
    expandRootNavSubmenus();
    bindSectionHeaderToggles();
  };

  if (window.jtd && typeof window.jtd.onReady === "function") {
    window.jtd.onReady(run);
  } else {
    // Defer: if this file loads before just-the-docs.js, jtd is missing but runs next; initNav must run first.
    setTimeout(run, 0);
  }
})();
