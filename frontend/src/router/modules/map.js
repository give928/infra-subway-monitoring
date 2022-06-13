const MapPage = () => import(/* webpackChunkName: "js/map/map" */'../../views/map/MapPage')

const mapRoutes = [
  {
    path: '/maps',
    component: MapPage
  }
]
export default mapRoutes
