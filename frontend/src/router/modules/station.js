const StationPage = () => import(/* webpackChunkName: "js/station/station" */'@/views/station/StationPage')

const stationRoutes = [
  {
    path: '/stations',
    component: StationPage
  }
]
export default stationRoutes
