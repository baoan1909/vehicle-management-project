interface AuthPageProps {
  mode?: "login" | "register" | "forgot" | "recover";
}

export function LoginPage({ mode = "login" }: AuthPageProps) {
  const isLogin = mode === "login";
  const isRegister = mode === "register";
  const isForgot = mode === "forgot";
  const isRecover = mode === "recover";
  const title = isLogin ? "WELCOME TO US" : isRegister ? "CREATE ACCOUNT" : "RECOVER PASSWORD";
  const message = isLogin ? "Đăng nhập để thỏa sức khám phá" : isRegister ? "Đăng ký tài khoản mới" : isForgot ? "Nhập email để nhận hướng dẫn khôi phục" : "Thiết lập mật khẩu mới";

  return (
    <div className="login-box">
      <div className="login-logo"><a href="#/pricing"><b>{title}</b></a></div>
      <div className="card"><div className="card-body login-card-body"><p className="login-box-msg">{message}</p><form>
        {isRegister && <div className="input-group mb-3"><input type="text" className="form-control" placeholder="Họ và tên" /><div className="input-group-append"><div className="input-group-text"><span className="fas fa-user-circle" /></div></div></div>}
        {(isLogin || isRegister || isForgot) && <div className="input-group mb-3"><input type="email" className="form-control" placeholder="Nhập Email" /><div className="input-group-append"><div className="input-group-text"><span className="fas fa-user" /></div></div></div>}
        {(isLogin || isRegister || isRecover) && <div className="input-group mb-3"><input type="password" className="form-control" placeholder="Mật khẩu" /><div className="input-group-append"><div className="input-group-text"><span className="fas fa-lock" /></div></div></div>}
        {isRecover && <div className="input-group mb-3"><input type="password" className="form-control" placeholder="Nhập lại mật khẩu" /><div className="input-group-append"><div className="input-group-text"><span className="fas fa-lock" /></div></div></div>}
        {isLogin && <div className="row"><div className="col-8"><div className="icheck-primary"><input type="checkbox" id="remember" /><label htmlFor="remember">Ghi nhớ mật khẩu</label></div></div></div>}
        <div className="col-12 mt-4 px-0"><button type="button" className="btn btn-primary btn-block">{isLogin ? "Đăng nhập" : isRegister ? "Đăng ký" : "Xác nhận"}</button></div>
      </form>
      {isLogin ? <><p className="mb-1 mt-3"><a href="#/forgot-password">Bạn quên mật khẩu?</a></p><p className="mb-0"><span>Bạn không có tài khoản?</span><a href="#/register" className="text-center"> Đăng ký</a></p></> : <p className="mb-0 mt-3"><a href="#/login">Quay lại đăng nhập</a></p>}
      </div></div>
    </div>
  );
}
