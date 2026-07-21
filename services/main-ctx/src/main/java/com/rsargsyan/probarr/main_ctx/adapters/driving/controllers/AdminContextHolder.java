package com.rsargsyan.probarr.main_ctx.adapters.driving.controllers;

public class AdminContextHolder {
  private static final ThreadLocal<AdminContext> adminContext = new ThreadLocal<>();

  public static void set(AdminContext ctx) {
    adminContext.set(ctx);
  }

  public static AdminContext get() {
    AdminContext ctx = adminContext.get();
    if (ctx == null) throw new IllegalStateException("AdminContext is not set");
    return ctx;
  }

  public static void clear() {
    adminContext.remove();
  }
}
