const LoginPage = () => import(/* webpackChunkName: "js/auth/login" */'@/views/auth/LoginPage')
const JoinPage = () => import(/* webpackChunkName: "js/auth/join" */'@/views/auth/JoinPage')
const Mypage = () => import(/* webpackChunkName: "js/auth/mypage" */'@/views/auth/Mypage')
const MypageEdit = () => import(/* webpackChunkName: "js/auth/mypage-edit" */'@/views/auth/MypageEdit')

const authRoutes = [
  {
    path: '/login',
    component: LoginPage
  },
  {
    path: '/join',
    component: JoinPage
  },
  {
    path: '/mypage',
    component: Mypage
  },
  {
    path: '/mypage/edit',
    component: MypageEdit
  }
]
export default authRoutes
