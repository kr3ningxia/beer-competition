package com.beercompetition.controller.portal;

import com.beercompetition.common.result.Result;
import com.beercompetition.pojo.enums.UserRole;
import com.beercompetition.pojo.vo.CurrentUserResponse;
import com.beercompetition.pojo.vo.EntryDetailVO;
import com.beercompetition.pojo.vo.EntrySummaryVO;
import com.beercompetition.service.AuthService;
import com.beercompetition.service.EntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal")
public class PortalController {

    private final AuthService authService;
    private final EntryService entryService;

    @GetMapping("/me")
    public Result<CurrentUserResponse> me() {
        return Result.success(authService.getCurrentUser(UserRole.PORTAL));
    }

    @GetMapping("/entries")
    public Result<List<EntrySummaryVO>> entries() {
        return Result.success(entryService.listPortalEntries());
    }

    @GetMapping("/entries/{id}")
    public Result<EntryDetailVO> entryDetail(@PathVariable Long id) {
        return Result.success(entryService.getPortalEntry(id));
    }
}
