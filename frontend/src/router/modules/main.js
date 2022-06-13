const MainPage = () => import(/* webpackChunkName: "js/main/main", webpackPrefetch: true */'@/views/main/MainPage')

const mainRoutes = [
  {
    path: '/',
    component: MainPage
  }
]
export default mainRoutes
