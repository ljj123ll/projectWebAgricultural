package com.agricultural.assistplatform.controller.user;

import com.agricultural.assistplatform.common.Result;
import com.agricultural.assistplatform.service.user.UserHomeService;
import com.agricultural.assistplatform.vo.user.HomeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端-首页与商品浏览")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserHomeController {

    private final UserHomeService userHomeService;

    @Operation(summary = "首页数据")
    @GetMapping("/home")
    public Result<HomeVO> home() {
        return Result.ok(userHomeService.getHome());
    }
}
