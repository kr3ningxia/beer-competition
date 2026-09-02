package com.beercompetition.service;

public interface EmailSender {

    /**
     * 发送一封 HTML 邮件，并返回供应商请求编号。
     */
    String send(String toAddress, String subject, String htmlBody);
}
