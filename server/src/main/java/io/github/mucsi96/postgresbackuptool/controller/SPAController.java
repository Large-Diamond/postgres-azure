package io.github.mucsi96.postgresbackuptool.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
public class SPAController {

  @GetMapping({ "/{segment1:[^.]*}", "/{segment1:.+}/{segment2:[^.]*}" })
  public String index(HttpServletRequest request, Model model) {
    String contextPath = request.getContextPath();
    model.addAttribute("baseHref", contextPath + "/");
    return "index";
  }
}
