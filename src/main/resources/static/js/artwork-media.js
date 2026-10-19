const displayChoice = { currency: "UAH", unit: "cm" };
for (const key of Object.keys(displayChoice)) {
  try {
    const value = localStorage.getItem("portfolio-" + key);
    if ((key === "currency" ? ["UAH", "USD"] : ["cm", "in"]).includes(value))
      displayChoice[key] = value;
  } catch (_) {
    /* Display preferences also work when browser storage is unavailable. */
  }
}
function applyDisplayChoice() {
  document.querySelectorAll("[data-preference]").forEach((select) => {
    select.value = displayChoice[select.dataset.preference];
  });
  document.querySelectorAll("[data-price]").forEach((element) => {
    const value = element.dataset[displayChoice.currency.toLowerCase()];
    element.textContent = value
      ? new Intl.NumberFormat(document.documentElement.lang, {
          style: "currency",
          currency: displayChoice.currency,
          maximumFractionDigits: 2,
        }).format(Number(value))
      : element.dataset.request;
  });
  document.querySelectorAll("[data-dimensions]").forEach((element) => {
    element.textContent = element.dataset[displayChoice.unit];
  });
}
document.querySelectorAll("[data-preference]").forEach((select) => {
  select.addEventListener("change", () => {
    const key = select.dataset.preference;
    displayChoice[key] = select.value;
    try {
      localStorage.setItem("portfolio-" + key, select.value);
    } catch (_) {
      /* Optional persistence. */
    }
    applyDisplayChoice();
  });
});
const galleryForm = document.querySelector("[data-gallery-filter]");
if (galleryForm) {
  const requestedCurrency = new URLSearchParams(location.search).get(
    "currency",
  );
  if (["UAH", "USD"].includes(requestedCurrency)) {
    displayChoice.currency = requestedCurrency;
    try {
      localStorage.setItem("portfolio-currency", requestedCurrency);
    } catch (_) {}
  } else if (displayChoice.currency !== "UAH") {
    const url = new URL(location.href);
    url.searchParams.set("currency", displayChoice.currency);
    location.replace(url);
  }
  const reset = galleryForm.querySelector("[data-reset-filters]");
  if (reset)
    reset.href = reset.pathname + "?currency=" + displayChoice.currency;
  const currencySelect = galleryForm.querySelector(
    '[data-preference="currency"]',
  );
  currencySelect.addEventListener("change", () => galleryForm.requestSubmit());
}
applyDisplayChoice();

const videoDialog = document.querySelector(".video-viewer");
const videoTrigger = document.querySelector("[data-video-open]");
if (
  videoDialog &&
  videoTrigger &&
  typeof videoDialog.showModal === "function"
) {
  const video = videoDialog.querySelector("video");
  videoTrigger.addEventListener("click", (event) => {
    if (
      event.ctrlKey ||
      event.metaKey ||
      event.shiftKey ||
      event.altKey ||
      event.button !== 0
    )
      return;
    event.preventDefault();
    video.src = videoTrigger.href;
    videoDialog.showModal();
    document.body.classList.add("viewer-open");
  });
  let startedOutside = false;
  const outside = (event) => {
    const rect = videoDialog.getBoundingClientRect();
    return (
      event.clientX < rect.left ||
      event.clientX > rect.right ||
      event.clientY < rect.top ||
      event.clientY > rect.bottom
    );
  };
  videoDialog.addEventListener("pointerdown", (event) => {
    startedOutside = outside(event);
  });
  videoDialog.addEventListener("pointerup", (event) => {
    if (startedOutside && outside(event)) videoDialog.close();
    startedOutside = false;
  });
  videoDialog.addEventListener("close", () => {
    video.pause();
    video.removeAttribute("src");
    video.load();
    document.body.classList.remove("viewer-open");
    videoTrigger.focus({ preventScroll: true });
  });
}

const inputUnit = document.querySelector("[data-input-unit]");
if (inputUnit) {
  let unit = inputUnit.value;
  inputUnit.addEventListener("change", () => {
    const factor = inputUnit.value === "in" ? 1 / 2.54 : 2.54;
    if (unit !== inputUnit.value) {
      ["widthCm", "heightCm"].forEach((id) => {
        const field = document.getElementById(id);
        if (field.value && Number.isFinite(Number(field.value)))
          field.value = (Number(field.value) * factor).toFixed(2);
      });
      unit = inputUnit.value;
    }
  });
}

const mediaForm = document.querySelector("[data-media-form]");
if (mediaForm) {
  const photos = mediaForm.querySelector("#additionalPhotos");
  const video = mediaForm.querySelector("#videoFile");
  let photoUrls = [];
  let videoUrl;
  function validatePhotos() {
    const removed = mediaForm.querySelectorAll(
      '[name="removePhotos"]:checked',
    ).length;
    const total =
      Number(photos.dataset.existing) - removed + photos.files.length;
    photos.setCustomValidity(
      total > 6
        ? photos.dataset.countError
        : [...photos.files].some((f) => f.size > 5 * 1024 * 1024)
          ? photos.dataset.sizeError
          : "",
    );
  }
  mediaForm
    .querySelectorAll('[name="removePhotos"]')
    .forEach((checkbox) => checkbox.addEventListener("change", validatePhotos));
  photos.addEventListener("change", () => {
    photoUrls.forEach(URL.revokeObjectURL);
    photoUrls = [];
    const container = mediaForm.querySelector(".pending-photos");
    container.replaceChildren();
    [...photos.files].forEach((file, index) => {
      const figure = document.createElement("figure");
      const img = document.createElement("img");
      const caption = document.createElement("figcaption");
      img.alt = file.name;
      if (
        file.size <= 5 * 1024 * 1024 &&
        ["image/jpeg", "image/png"].includes(file.type)
      ) {
        const url = URL.createObjectURL(file);
        photoUrls.push(url);
        img.src = url;
      }
      caption.textContent = file.name;
      const remove = document.createElement("button");
      remove.type = "button";
      remove.className = "pending-remove";
      remove.title = photos.dataset.removeLabel;
      remove.setAttribute(
        "aria-label",
        photos.dataset.removeLabel + ": " + file.name,
      );
      const icon = document.createElement("i");
      icon.dataset.lucide = "x";
      icon.setAttribute("aria-hidden", "true");
      remove.append(icon);
      remove.addEventListener("click", () => {
        const selection = new DataTransfer();
        [...photos.files]
          .filter((_, i) => i !== index)
          .forEach((f) => selection.items.add(f));
        photos.files = selection.files;
        photos.dispatchEvent(new Event("change"));
      });
      figure.append(img, caption, remove);
      container.append(figure);
    });
    if (window.lucide) window.lucide.createIcons();
    validatePhotos();
  });
  video.addEventListener("change", () => {
    const file = video.files[0];
    video.setCustomValidity(
      file && file.size > 100 * 1024 * 1024 ? video.dataset.sizeError : "",
    );
    const preview = mediaForm.querySelector(".pending-video");
    preview.pause();
    preview.removeAttribute("src");
    if (videoUrl) URL.revokeObjectURL(videoUrl);
    preview.hidden = !file || file.size > 100 * 1024 * 1024;
    if (!preview.hidden) {
      videoUrl = URL.createObjectURL(file);
      preview.src = videoUrl;
    }
    preview.load();
  });
  mediaForm.addEventListener(
    "submit",
    () => (mediaForm.querySelector('button[type="submit"]').disabled = true),
  );
  window.addEventListener("pageshow", () => {
    mediaForm.querySelector('button[type="submit"]').disabled = false;
  });
}
