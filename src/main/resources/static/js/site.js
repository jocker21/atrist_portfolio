const mobileHeader = document.querySelector(".site-header");
const menuToggle = mobileHeader?.querySelector(".mobile-menu-toggle");
if (menuToggle) {
  const mobileLayout = window.matchMedia("(max-width: 700px)");
  const setMenuOpen = (open) => {
    mobileHeader.classList.toggle("is-menu-open", open);
    menuToggle.setAttribute("aria-expanded", String(open));
  };
  mobileHeader.classList.add("nav-ready");
  menuToggle.addEventListener("click", () => {
    setMenuOpen(menuToggle.getAttribute("aria-expanded") !== "true");
  });
  mobileHeader.querySelector(".site-nav").addEventListener("click", (event) => {
    if (event.target.closest("a")) setMenuOpen(false);
  });
  document.addEventListener("click", (event) => {
    if (!mobileHeader.contains(event.target)) setMenuOpen(false);
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && menuToggle.getAttribute("aria-expanded") === "true") {
      setMenuOpen(false);
      menuToggle.focus();
    }
  });
  mobileHeader.addEventListener("focusout", (event) => {
    if (!mobileHeader.contains(event.relatedTarget)) setMenuOpen(false);
  });
  mobileLayout.addEventListener("change", () => setMenuOpen(false));
}

window.addEventListener("DOMContentLoaded", () => {
  if (window.lucide) window.lucide.createIcons();
});

const viewer = document.querySelector(".image-viewer");
const previews = [...document.querySelectorAll("[data-image-preview]")];
if (viewer && previews.length && typeof viewer.showModal === "function") {
  const image = viewer.querySelector(".viewer-image");
  const previous = viewer.querySelector("[data-viewer-previous]");
  const next = viewer.querySelector("[data-viewer-next]");
  let index = 0;
  let trigger = previews[0];
  const show = (position) => {
    index = Math.max(0, Math.min(position, previews.length - 1));
    image.classList.remove("broken-image");
    image.src = previews[index].href;
    image.alt = previews[index].querySelector("img").alt;
    previous.disabled = index === 0;
    next.disabled = index === previews.length - 1;
    viewer.querySelector("[data-viewer-counter]").textContent =
      index + 1 + " / " + previews.length;
    viewer.querySelector(".viewer-navigation").hidden = previews.length < 2;
  };
  previews.forEach((preview, position) => {
    preview.addEventListener("click", (event) => {
      if (
        event.ctrlKey ||
        event.metaKey ||
        event.shiftKey ||
        event.altKey ||
        event.button !== 0
      )
        return;
      if (!preview.querySelector("img").naturalWidth) return;
      event.preventDefault();
      trigger = preview;
      show(position);
      viewer.showModal();
      document.body.classList.add("viewer-open");
    });
  });
  previous.addEventListener("click", () => show(index - 1));
  next.addEventListener("click", () => show(index + 1));
  viewer.addEventListener("keydown", (event) => {
    if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
      event.preventDefault();
      show(index + (event.key === "ArrowRight" ? 1 : -1));
    }
  });
  const outside = (event) => {
    const rect = viewer.getBoundingClientRect();
    return (
      event.clientX < rect.left ||
      event.clientX > rect.right ||
      event.clientY < rect.top ||
      event.clientY > rect.bottom
    );
  };
  let startedOutside = false;
  viewer.addEventListener("pointerdown", (event) => {
    startedOutside = outside(event);
  });
  viewer.addEventListener("pointerup", (event) => {
    if (startedOutside && outside(event)) viewer.close();
    startedOutside = false;
  });
  viewer.addEventListener("close", () => {
    document.body.classList.remove("viewer-open");
    trigger.focus({ preventScroll: true });
  });
}

document.querySelectorAll("img").forEach((image) => {
  const showUnavailable = () => {
    image.classList.add("broken-image");
    image.alt =
      document.documentElement.lang === "en"
        ? "Image unavailable"
        : "Зображення недоступне";
    image.removeAttribute("src");
  };
  image.addEventListener("error", showUnavailable, { once: true });
  if (image.complete && image.getAttribute("src") && image.naturalWidth === 0)
    showUnavailable();
});
document.querySelectorAll("form[data-confirm]").forEach((form) => {
  form.addEventListener("submit", (event) => {
    if (!window.confirm(form.dataset.confirm)) event.preventDefault();
  });
});
