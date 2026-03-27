package com.example.artist.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/contact")
public class ContactController {

    // GET /contact — показать форму
    @GetMapping
    public String contactForm(Model model) {
        // Передаём пустой объект формы в шаблон
        // Thymeleaf привяжет поля формы к полям этого объекта
        model.addAttribute("contactForm", new ContactForm());
        return "contact";
    }

    // POST /contact — обработать отправленную форму
    @PostMapping
    public String submitContact(
            // @Valid — запускает валидацию полей формы
            // @ModelAttribute — Spring заполняет объект данными из формы
            @Valid @ModelAttribute("contactForm") ContactForm form,
            // BindingResult — содержит ошибки валидации (если есть)
            // ВАЖНО: BindingResult должен идти сразу после @Valid-параметра
            BindingResult bindingResult,
            Model model) {

        // Если есть ошибки валидации — вернуть форму с сообщениями об ошибках
        if (bindingResult.hasErrors()) {
            return "contact";
        }

        // TODO: здесь будет отправка email через Spring Mail
        // Пока просто логируем
        System.out.println("Новое сообщение от: " + form.getEmail());
        System.out.println("Текст: " + form.getMessage());

        // Передаём флаг успеха в шаблон для показа сообщения
        model.addAttribute("success", true);
        return "contact";
    }

    // Вложенный класс — DTO (Data Transfer Object) для формы контактов.
    // Хранит данные формы и правила валидации.
    @Data // Lombok: генерирует getters, setters, toString
    public static class ContactForm {

        @NotBlank(message = "Введите ваше имя")
        private String name;

        @NotBlank(message = "Введите email")
        @Email(message = "Некорректный email")
        private String email;

        @NotBlank(message = "Введите сообщение")
        private String message;
    }
}
