const PathPage = () => import(/* webpackChunkName: "js/path/path" */'../../views/path/PathPage')

const pathRoutes = [
  {
    path: '/path',
    component: PathPage
  }
]
export default pathRoutes
