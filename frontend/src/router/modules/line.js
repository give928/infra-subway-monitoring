const LinePage = () => import(/* webpackChunkName: "js/line/line" */'@/views/line/LinePage')

const lineRoutes = [
  {
    path: '/lines',
    component: LinePage
  }
]
export default lineRoutes
