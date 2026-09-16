package com.beercompetition.service;

import com.beercompetition.pojo.vo.FileDownloadVO;
import org.springframework.web.multipart.MultipartFile;

public interface JudgeAvatarService {

    Long uploadMyAvatar(MultipartFile file);

    FileDownloadVO downloadMyAvatar();
}
