window.addEventListener("DOMContentLoaded", () => {
  if (window.lucide) window.lucide.createIcons();
});

const viewer = document.querySelector(".image-viewer");
const preview = document.querySelector("[data-image-preview]");
if (viewer && preview && typeof viewer.showModal === "function") {
  const viewerImage = viewer.querySelector(".viewer-image");
  preview.addEventListener("click", (event) => {
    if (
      event.ctrlKey ||
      event.metaKey ||
      event.shiftKey ||
      event.altKey ||
      event.button !== 0
    )
      return;
    const original = preview.querySelector("img");
    if (!original.naturalWidth) return;
    event.preventDefault();
    viewerImage.src = preview.href;
    viewer.showModal();
    document.body.classList.add("viewer-open");
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
    preview.focus({ preventScroll: true });
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
