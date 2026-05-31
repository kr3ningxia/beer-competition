package com.beercompetition.storage;

public interface FileStorageService {

    String upload(String businessType, String fileName, byte[] bytes);
}
