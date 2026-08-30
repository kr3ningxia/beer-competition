package com.beercompetition.controller.judge;
import com.beercompetition.common.result.Result; import com.beercompetition.pojo.dto.JudgeRecruitmentApplicationRequest; import com.beercompetition.pojo.vo.*; import com.beercompetition.service.JudgeRecruitmentService; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequiredArgsConstructor @RequestMapping("/api/judge/recruitments")
public class JudgeRecruitmentController { private final JudgeRecruitmentService service;
 @GetMapping public Result<List<JudgeRecruitmentVO>> list(){return Result.success(service.publicList());}
 @GetMapping("/{id}") public Result<JudgeRecruitmentVO> get(@PathVariable Long id){return Result.success(service.publicGet(id));}
 @PostMapping("/{id}/applications") public Result<JudgeRecruitmentApplicationVO> apply(@PathVariable Long id,@RequestBody @Valid JudgeRecruitmentApplicationRequest q){return Result.success(service.apply(id,q));}
 @GetMapping("/applications") public Result<List<JudgeRecruitmentVO>> mine(){return Result.success(service.myApplications());}
 @PutMapping("/applications/{id}") public Result<JudgeRecruitmentApplicationVO> update(@PathVariable Long id,@RequestBody @Valid JudgeRecruitmentApplicationRequest q){return Result.success(service.updateApplication(id,q));}
 @PostMapping("/applications/{id}/withdraw") public Result<JudgeRecruitmentApplicationVO> withdraw(@PathVariable Long id){return Result.success(service.withdraw(id));}
}
